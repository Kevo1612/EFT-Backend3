package cl.bancoxyz.cuentas.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bancoxyz.common.security.SecurityUtils;
import cl.bancoxyz.common.web.NegocioException;
import cl.bancoxyz.cuentas.client.ClientesClient.ClienteDto;
import cl.bancoxyz.cuentas.domain.Cuenta;
import cl.bancoxyz.cuentas.domain.Cuenta.Estado;
import cl.bancoxyz.cuentas.domain.MovimientoCuenta;
import cl.bancoxyz.cuentas.dto.AbrirCuentaRequest;
import cl.bancoxyz.cuentas.dto.CuentaResponse;
import cl.bancoxyz.cuentas.dto.MovimientoRequest;
import cl.bancoxyz.cuentas.dto.MovimientoResponse;
import cl.bancoxyz.cuentas.kafka.PublicadorEventos;
import cl.bancoxyz.cuentas.repository.CuentaRepository;
import cl.bancoxyz.cuentas.repository.MovimientoCuentaRepository;

@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    private final CuentaRepository cuentas;
    private final MovimientoCuentaRepository movimientos;
    private final VerificadorCliente verificador;
    private final PublicadorEventos eventos;

    public CuentaService(CuentaRepository cuentas, MovimientoCuentaRepository movimientos,
                         VerificadorCliente verificador, PublicadorEventos eventos) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.verificador = verificador;
        this.eventos = eventos;
    }

    // ------------------------------------------------------------------ apertura

    public CuentaResponse abrir(AbrirCuentaRequest req) {
        boolean esStaff = SecurityUtils.tieneRol("ADMIN") || SecurityUtils.tieneRol("SERVICIO");
        ClienteDto cliente = verificador.obtener(req.clienteId());

        Cuenta cuenta = new Cuenta();
        cuenta.setClienteId(req.clienteId());
        cuenta.setTipo(req.tipo());
        cuenta.setNumeroCuenta(generarNumero());

        if (cliente == null) {
            // ms-clientes caido: un ejecutivo puede abrir igual (queda pendiente), un cliente no
            if (!esStaff) {
                throw NegocioException.noDisponible("No es posible validar su identidad ahora, intente mas tarde");
            }
            cuenta.setTitularUsername("pendiente");
            cuenta.setEstado(Estado.PENDIENTE_VALIDACION);
        } else {
            if (!cliente.activo()) {
                throw NegocioException.conflicto("El cliente " + req.clienteId() + " esta inactivo");
            }
            if (!esStaff && !cliente.username().equals(SecurityUtils.usuarioActual())) {
                throw NegocioException.prohibido("Solo puede abrir cuentas a su nombre");
            }
            cuenta.setTitularUsername(cliente.username());
            cuenta.setEstado(Estado.ACTIVA);
        }
        Cuenta guardada = cuentas.save(cuenta);
        eventos.cuenta(guardada, "ABIERTA", "Estado inicial " + guardada.getEstado());
        return CuentaResponse.de(guardada);
    }

    /** Reintenta validar las cuentas que quedaron pendientes cuando ms-clientes estaba caido. */
    @Transactional
    public void validarPendientes() {
        for (Cuenta c : cuentas.findByEstado(Estado.PENDIENTE_VALIDACION)) {
            ClienteDto cliente = verificador.obtener(c.getClienteId());
            if (cliente == null) {
                return; // sigue caido, se intentara en la proxima ejecucion
            }
            c.setTitularUsername(cliente.username());
            c.setEstado(cliente.activo() ? Estado.ACTIVA : Estado.BLOQUEADA);
            eventos.cuenta(c, "VALIDADA", "Resultado " + c.getEstado());
            log.info("Cuenta {} validada -> {}", c.getNumeroCuenta(), c.getEstado());
        }
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public CuentaResponse obtener(Long id) {
        Cuenta c = buscar(id);
        verificarPropiedad(c);
        return CuentaResponse.de(c);
    }

    @Transactional(readOnly = true)
    public List<CuentaResponse> porCliente(Long clienteId) {
        List<Cuenta> lista = cuentas.findByClienteIdOrderByIdAsc(clienteId);
        lista.forEach(this::verificarPropiedad);
        return lista.stream().map(CuentaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> movimientos(Long cuentaId, int limite) {
        verificarPropiedad(buscar(cuentaId));
        return movimientos.findByCuentaIdOrderByFechaDesc(cuentaId, PageRequest.of(0, Math.min(limite, 200)))
                .stream().map(MovimientoResponse::de).toList();
    }

    // ------------------------------------------------------------------ movimientos de saldo

    /**
     * Aplica un cargo o abono de forma atomica e idempotente.
     * 1) Bloquea la fila de la cuenta (FOR UPDATE) -> serializa operaciones sobre la misma cuenta.
     * 2) Si la referencia ya existe devuelve el movimiento original (reintento seguro).
     * 3) Valida estado y saldo, actualiza y registra el asiento en la misma transaccion.
     */
    @Transactional
    public MovimientoResponse aplicarMovimiento(Long cuentaId, MovimientoRequest req) {
        Cuenta c = cuentas.findByIdForUpdate(cuentaId)
                .orElseThrow(() -> NegocioException.noEncontrado("Cuenta " + cuentaId + " no existe"));

        var existente = movimientos.findByReferencia(req.referencia());
        if (existente.isPresent()) {
            if (!existente.get().getCuentaId().equals(cuentaId)) {
                throw NegocioException.conflicto("Referencia " + req.referencia() + " ya usada en otra cuenta");
            }
            log.info("Movimiento {} ya aplicado, se devuelve el existente", req.referencia());
            return MovimientoResponse.de(existente.get());
        }

        boolean esDebito = req.tipo() == MovimientoCuenta.Tipo.DEBITO;
        boolean operativa = c.getEstado() == Estado.ACTIVA
                || (!esDebito && c.getEstado() == Estado.BLOQUEADA); // devoluciones se aceptan en cuentas bloqueadas
        if (!operativa) {
            throw NegocioException.conflicto("La cuenta " + c.getNumeroCuenta() + " no permite la operacion (estado "
                    + c.getEstado() + ")");
        }

        BigDecimal nuevoSaldo;
        if (esDebito) {
            if (c.getSaldo().compareTo(req.monto()) < 0) {
                throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente");
            }
            nuevoSaldo = c.getSaldo().subtract(req.monto());
        } else {
            nuevoSaldo = c.getSaldo().add(req.monto());
        }
        c.setSaldo(nuevoSaldo);
        MovimientoCuenta mov = movimientos.save(new MovimientoCuenta(cuentaId, req.referencia(), req.tipo(),
                req.monto(), nuevoSaldo, req.descripcion()));
        return MovimientoResponse.de(mov);
    }

    // ------------------------------------------------------------------ mantenimiento

    @Transactional
    public CuentaResponse bloquear(Long id, String motivo) {
        Cuenta c = buscar(id);
        if (c.getEstado() == Estado.CERRADA || c.getEstado() == Estado.BLOQUEADA) {
            return CuentaResponse.de(c);
        }
        c.setEstado(Estado.BLOQUEADA);
        eventos.cuenta(c, "BLOQUEADA", motivo);
        return CuentaResponse.de(c);
    }

    @Transactional
    public CuentaResponse desbloquear(Long id) {
        Cuenta c = buscar(id);
        if (c.getEstado() != Estado.BLOQUEADA) {
            throw NegocioException.conflicto("La cuenta no esta bloqueada");
        }
        c.setEstado(Estado.ACTIVA);
        eventos.cuenta(c, "DESBLOQUEADA", "Desbloqueo manual por " + SecurityUtils.usuarioActual());
        return CuentaResponse.de(c);
    }

    @Transactional
    public CuentaResponse cerrar(Long id) {
        Cuenta c = buscar(id);
        verificarPropiedad(c);
        if (c.getEstado() == Estado.CERRADA) {
            throw NegocioException.conflicto("La cuenta ya esta cerrada");
        }
        if (c.getSaldo().signum() != 0) {
            throw NegocioException.conflicto("La cuenta debe tener saldo 0 para cerrarse (saldo actual "
                    + c.getSaldo().toPlainString() + ")");
        }
        c.setEstado(Estado.CERRADA);
        c.setFechaCierre(Instant.now());
        eventos.cuenta(c, "CERRADA", "Cierre solicitado por " + SecurityUtils.usuarioActual());
        return CuentaResponse.de(c);
    }

    // ------------------------------------------------------------------ helpers

    private Cuenta buscar(Long id) {
        return cuentas.findById(id).orElseThrow(() -> NegocioException.noEncontrado("Cuenta " + id + " no existe"));
    }

    private void verificarPropiedad(Cuenta c) {
        if (SecurityUtils.tieneRol("ADMIN") || SecurityUtils.tieneRol("SERVICIO")) {
            return;
        }
        if (!c.getTitularUsername().equals(SecurityUtils.usuarioActual())) {
            throw NegocioException.prohibido("La cuenta no pertenece al usuario autenticado");
        }
    }

    private String generarNumero() {
        String numero;
        do {
            numero = "XYZ-" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
        } while (cuentas.existsByNumeroCuenta(numero));
        return numero;
    }
}
