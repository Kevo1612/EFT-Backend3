package cl.bancoxyz.pagos.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import cl.bancoxyz.common.security.SecurityUtils;
import cl.bancoxyz.common.web.NegocioException;
import cl.bancoxyz.pagos.client.CuentasClient.CuentaDto;
import cl.bancoxyz.pagos.domain.Pago;
import cl.bancoxyz.pagos.domain.Pago.Estado;
import cl.bancoxyz.pagos.domain.Pago.Tipo;
import cl.bancoxyz.pagos.dto.PagoRequest;
import cl.bancoxyz.pagos.dto.PagoResponse;
import cl.bancoxyz.pagos.kafka.PublicadorEventos;
import cl.bancoxyz.pagos.repository.PagoRepository;

/**
 * Orquestador de operaciones monetarias (patron SAGA orquestada).
 *
 * Paso 1 validar cuentas  ->  Paso 2 debitar origen  ->  Paso 3 acreditar destino
 *                                                        |-- si falla por negocio: COMPENSAR (devolver al origen)
 * Cada paso se registra en la entidad Pago antes de seguir, y los movimientos en ms-cuentas son
 * idempotentes por referencia, por lo que el proceso puede reanudarse desde cualquier punto.
 * Si ms-cuentas no esta disponible el pago queda PENDIENTE_REINTENTO y un proceso programado
 * lo retoma (consistencia eventual en lugar de transaccion distribuida 2PC).
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository repo;
    private final CuentasGateway cuentas;
    private final PublicadorEventos eventos;
    private final DetectorFraude fraude;
    private final int maxIntentos;

    public PagoService(PagoRepository repo, CuentasGateway cuentas, PublicadorEventos eventos, DetectorFraude fraude,
                       @Value("${banco.pagos.max-intentos:5}") int maxIntentos) {
        this.repo = repo;
        this.cuentas = cuentas;
        this.eventos = eventos;
        this.fraude = fraude;
        this.maxIntentos = maxIntentos;
    }

    // ------------------------------------------------------------------ entrada

    public Pago procesar(Tipo tipo, PagoRequest req, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var previo = repo.findByIdempotencyKey(idempotencyKey);
            if (previo.isPresent()) {
                log.info("Idempotency-Key {} repetida, se devuelve el pago {}", idempotencyKey,
                        previo.get().getReferencia());
                return previo.get();
            }
        }
        validarForma(tipo, req);

        Pago p = new Pago();
        p.setReferencia(UUID.randomUUID().toString());
        p.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);
        p.setTipo(tipo);
        p.setCuentaOrigenId(tipo == Tipo.DEPOSITO ? null : req.cuentaOrigenId());
        p.setCuentaDestinoId(tipo == Tipo.TRANSFERENCIA || tipo == Tipo.DEPOSITO ? req.cuentaDestinoId() : null);
        p.setMonto(req.monto());
        p.setDescripcion(req.descripcion() != null ? req.descripcion() : tipo.name().toLowerCase());
        p.setCanal(req.canal() != null ? req.canal().toUpperCase() : "API");
        p.setSolicitante(SecurityUtils.usuarioActual());
        p.setSolicitanteStaff(SecurityUtils.tieneRol("ADMIN") || SecurityUtils.tieneRol("SERVICIO"));
        p = repo.save(p);
        log.info("Pago {} creado tipo={} monto={} canal={}", p.getReferencia(), tipo, p.getMonto(), p.getCanal());
        return ejecutar(p);
    }

    // ------------------------------------------------------------------ saga

    Pago ejecutar(Pago p) {
        p.setIntentos(p.getIntentos() + 1);
        try {
            if (!p.isValidado()) {
                validarCuentas(p);
                p.setValidado(true);
                p = repo.save(p);
            }
            if (p.getCuentaOrigenId() != null && !p.isDebitoAplicado()) {
                cuentas.debitar(p.getCuentaOrigenId(), p.getReferencia() + "-D", p.getMonto(), p.getDescripcion());
                p.setDebitoAplicado(true);
                p = repo.save(p);
            }
            if (p.getCuentaDestinoId() != null && !p.isCreditoAplicado()) {
                try {
                    cuentas.acreditar(p.getCuentaDestinoId(), p.getReferencia() + "-C", p.getMonto(),
                            p.getDescripcion());
                    p.setCreditoAplicado(true);
                } catch (OperacionRechazadaException rechazo) {
                    if (p.isDebitoAplicado()) {
                        return compensar(p, rechazo.getMessage());
                    }
                    throw rechazo;
                }
            }
            p.setEstado(Estado.COMPLETADO);
            p.setMotivo(null);
            p = repo.save(p);
            log.info("Pago {} COMPLETADO en intento {}", p.getReferencia(), p.getIntentos());
            eventos.transaccion(p);
            fraude.evaluar(p);
            return p;

        } catch (OperacionRechazadaException rechazo) {
            p.setEstado(Estado.RECHAZADO);
            p.setMotivo(rechazo.getMessage());
            p = repo.save(p);
            log.info("Pago {} RECHAZADO: {}", p.getReferencia(), rechazo.getMessage());
            eventos.transaccion(p);
            return p;

        } catch (ServicioNoDisponibleException caido) {
            if ("CAJERO".equals(p.getCanal())) {
                // En cajero no se difiere: el cliente esta frente a la maquina y no se le entrega dinero.
                // Si el cargo alcanzo a aplicarse, el proceso de conciliacion de cajeros lo revierte.
                p.setEstado(Estado.REQUIERE_REVISION);
                p.setMotivo("Operacion de cajero no confirmada; pasa a conciliacion");
                p = repo.save(p);
                log.warn("Pago {} de cajero no confirmado -> REQUIERE_REVISION", p.getReferencia());
                return p;
            }
            boolean agotado = p.getIntentos() >= maxIntentos;
            p.setEstado(agotado ? Estado.REQUIERE_REVISION : Estado.PENDIENTE_REINTENTO);
            p.setMotivo(agotado
                    ? "Se agotaron los reintentos automaticos; requiere revision de operaciones"
                    : "Servicio de cuentas no disponible; se reintentara automaticamente");
            p = repo.save(p);
            log.warn("Pago {} -> {} (intento {}/{})", p.getReferencia(), p.getEstado(), p.getIntentos(), maxIntentos);
            return p;
        }
    }

    /** Transaccion compensatoria: devuelve al origen el monto ya debitado. */
    private Pago compensar(Pago p, String motivo) {
        try {
            cuentas.acreditar(p.getCuentaOrigenId(), p.getReferencia() + "-R", p.getMonto(),
                    "Reverso: " + p.getDescripcion());
        } catch (OperacionRechazadaException e) {
            // No deberia ocurrir (los abonos se aceptan incluso en cuentas bloqueadas); se escala a operaciones
            p.setEstado(Estado.REQUIERE_REVISION);
            p.setMotivo("Fallo la compensacion: " + e.getMessage());
            log.error("Pago {} no pudo compensarse: {}", p.getReferencia(), e.getMessage());
            return repo.save(p);
        }
        p.setEstado(Estado.REVERTIDO);
        p.setMotivo("Destino rechazo el abono (" + motivo + "). Monto devuelto a la cuenta de origen");
        p = repo.save(p);
        log.warn("Pago {} REVERTIDO: {}", p.getReferencia(), motivo);
        eventos.transaccion(p);
        return p;
    }

    private void validarCuentas(Pago p) {
        CuentaDto principal = cuentas.obtener(p.cuentaPrincipal());
        if (!p.isSolicitanteStaff() && !p.getSolicitante().equals(principal.titularUsername())) {
            throw new OperacionRechazadaException(403, "La cuenta " + principal.numeroCuenta()
                    + " no pertenece al usuario " + p.getSolicitante());
        }
        p.setClienteId(principal.clienteId());
        if (p.getTipo() == Tipo.TRANSFERENCIA) {
            CuentaDto destino = cuentas.obtener(p.getCuentaDestinoId()); // 404 -> rechazo
            if (!"ACTIVA".equals(destino.estado())) {
                throw new OperacionRechazadaException(409, "La cuenta de destino no esta activa");
            }
        }
    }

    private void validarForma(Tipo tipo, PagoRequest req) {
        switch (tipo) {
            case TRANSFERENCIA -> {
                if (req.cuentaOrigenId() == null || req.cuentaDestinoId() == null) {
                    throw NegocioException.invalido("Transferencia requiere cuentaOrigenId y cuentaDestinoId");
                }
                if (req.cuentaOrigenId().equals(req.cuentaDestinoId())) {
                    throw NegocioException.invalido("La cuenta de origen y destino no pueden ser la misma");
                }
            }
            case DEPOSITO -> {
                if (req.cuentaDestinoId() == null) {
                    throw NegocioException.invalido("Deposito requiere cuentaDestinoId");
                }
            }
            case RETIRO, PAGO_SERVICIO -> {
                if (req.cuentaOrigenId() == null) {
                    throw NegocioException.invalido(tipo + " requiere cuentaOrigenId");
                }
            }
        }
    }

    // ------------------------------------------------------------------ reproceso

    /** Retoma los pagos que quedaron pendientes porque ms-cuentas estaba caido. */
    public void reprocesarPendientes() {
        List<Pago> pendientes = repo.findTop50ByEstadoOrderByFechaCreacionAsc(Estado.PENDIENTE_REINTENTO);
        for (Pago pendiente : pendientes) {
            if (!repo.reclamar(pendiente.getId())) {
                continue; // otra replica lo tomo
            }
            repo.findById(pendiente.getId()).ifPresent(p -> {
                log.info("Reprocesando pago {} (intento {})", p.getReferencia(), p.getIntentos() + 1);
                ejecutar(p);
            });
        }
    }

    // ------------------------------------------------------------------ consultas

    public PagoResponse obtener(String referencia) {
        Pago p = repo.findByReferencia(referencia)
                .orElseThrow(() -> NegocioException.noEncontrado("Pago " + referencia + " no existe"));
        if (!SecurityUtils.tieneRol("ADMIN") && !SecurityUtils.tieneRol("SERVICIO")
                && !p.getSolicitante().equals(SecurityUtils.usuarioActual())) {
            throw NegocioException.prohibido("El pago no pertenece al usuario autenticado");
        }
        return PagoResponse.de(p);
    }

    /** Historial de una cuenta. La propiedad de la cuenta se valida contra ms-cuentas. */
    public List<PagoResponse> historial(Long cuentaId, int limite) {
        if (!SecurityUtils.tieneRol("ADMIN") && !SecurityUtils.tieneRol("SERVICIO")) {
            CuentaDto cuenta = cuentas.obtener(cuentaId);
            if (!SecurityUtils.usuarioActual().equals(cuenta.titularUsername())) {
                throw NegocioException.prohibido("La cuenta no pertenece al usuario autenticado");
            }
        }
        return repo.historialCuenta(cuentaId, PageRequest.of(0, Math.min(limite, 100))).stream()
                .map(PagoResponse::de).toList();
    }
}
