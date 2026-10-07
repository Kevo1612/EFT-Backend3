package cl.bancoxyz.bffcajero.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.bancoxyz.bffcajero.client.Clients.CuentasClient;
import cl.bancoxyz.bffcajero.client.Clients.PagosClient;
import cl.bancoxyz.bffcajero.config.CajeroProperties;
import cl.bancoxyz.bffcajero.dto.Cajero;
import cl.bancoxyz.common.security.SecurityUtils;
import cl.bancoxyz.common.web.NegocioException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Canal cajero: operaciones criticas.
 * Politica: NUNCA se responde con datos en cache ni se asume exito. Si el backend falla,
 * el cajero recibe 503 y no entrega dinero. Los retiros exigen Idempotency-Key para que un
 * reintento del cajero (p.ej. por corte de red) no genere un doble cargo.
 */
@Service
public class CajeroService {

    private static final Logger audit = LoggerFactory.getLogger("AUDITORIA_CAJERO");

    private final CuentasClient cuentas;
    private final PagosClient pagos;
    private final CajeroProperties props;

    public CajeroService(CuentasClient cuentas, PagosClient pagos, CajeroProperties props) {
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.props = props;
    }

    @CircuitBreaker(name = "ms-cuentas", fallbackMethod = "saldoFallback")
    public Cajero.Saldo saldo(Long cuentaId, String cajero) {
        Cajero.CuentaDto c = cuentas.obtener(cuentaId); // ms-cuentas valida que sea del titular del token
        audit.info("SALDO usuario={} cuenta={} cajero={}", SecurityUtils.usuarioActual(), cuentaId, cajero);
        return new Cajero.Saldo(enmascarar(c.numeroCuenta()), c.saldo(), Instant.now());
    }

    @CircuitBreaker(name = "ms-pagos", fallbackMethod = "operacionFallback")
    public Cajero.ComprobanteOperacion retirar(Cajero.Operacion op, String cajero, String idempotencyKey) {
        validarMonto(op.monto());
        Cajero.PagoDto p = pagos.retirar(new Cajero.PagoInterno(op.cuentaId(), null, op.monto(),
                "Retiro cajero " + cajero, "CAJERO"), clave(cajero, idempotencyKey));
        audit.info("RETIRO usuario={} cuenta={} monto={} cajero={} ref={} estado={}", SecurityUtils.usuarioActual(),
                op.cuentaId(), op.monto(), cajero, p.referencia(), p.estado());
        return comprobante("RETIRO", p, op, cajero);
    }

    @CircuitBreaker(name = "ms-pagos", fallbackMethod = "operacionFallback")
    public Cajero.ComprobanteOperacion depositar(Cajero.Operacion op, String cajero, String idempotencyKey) {
        if (op.monto().remainder(props.multiplo()).signum() != 0) {
            throw NegocioException.invalido("El deposito en efectivo debe ser multiplo de $" + props.multiplo());
        }
        Cajero.PagoDto p = pagos.depositar(new Cajero.PagoInterno(null, op.cuentaId(), op.monto(),
                "Deposito cajero " + cajero, "CAJERO"), clave(cajero, idempotencyKey));
        audit.info("DEPOSITO usuario={} cuenta={} monto={} cajero={} ref={} estado={}", SecurityUtils.usuarioActual(),
                op.cuentaId(), op.monto(), cajero, p.referencia(), p.estado());
        return comprobante("DEPOSITO", p, op, cajero);
    }

    // ---------------------------------------------------------------- helpers

    private Cajero.ComprobanteOperacion comprobante(String tipo, Cajero.PagoDto p, Cajero.Operacion op, String cajero) {
        BigDecimal saldo = null;
        String cuenta = null;
        if ("COMPLETADO".equals(p.estado())) {
            try {
                Cajero.CuentaDto c = cuentas.obtener(op.cuentaId());
                saldo = c.saldo();
                cuenta = enmascarar(c.numeroCuenta());
            } catch (RuntimeException e) {
                // el saldo en el comprobante es informativo: si falla no se invalida la operacion
            }
        }
        String mensaje = switch (p.estado()) {
            case "COMPLETADO" -> "RETIRO".equals(tipo) ? "Retire su dinero" : "Deposito acreditado";
            case "REQUIERE_REVISION", "PENDIENTE_REINTENTO" ->
                    "No fue posible confirmar la operacion. No se entrego dinero; cualquier cargo sera reversado";
            default -> p.motivo();
        };
        return new Cajero.ComprobanteOperacion(p.referencia(), tipo, p.estado(), op.monto(), cuenta, saldo, cajero,
                Instant.now(), mensaje);
    }

    private void validarMonto(BigDecimal monto) {
        if (monto.compareTo(props.montoMinimo()) < 0 || monto.compareTo(props.montoMaximo()) > 0) {
            throw NegocioException.invalido("El monto debe estar entre $" + props.montoMinimo() + " y $"
                    + props.montoMaximo());
        }
        if (monto.remainder(props.multiplo()).signum() != 0) {
            throw NegocioException.invalido("El monto debe ser multiplo de $" + props.multiplo());
        }
    }

    /** La clave de idempotencia se asocia al cajero para que dos terminales no colisionen. */
    private static String clave(String cajero, String idempotencyKey) {
        return cajero + ":" + idempotencyKey;
    }

    static String enmascarar(String numero) {
        return numero == null || numero.length() < 4 ? numero : "****" + numero.substring(numero.length() - 4);
    }

    @SuppressWarnings("unused")
    private Cajero.Saldo saldoFallback(Long cuentaId, String cajero, Throwable e) {
        throw traducir(e);
    }

    @SuppressWarnings("unused")
    private Cajero.ComprobanteOperacion operacionFallback(Cajero.Operacion op, String cajero, String key, Throwable e) {
        throw traducir(e);
    }

    private RuntimeException traducir(Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            return fce;
        }
        if (e instanceof NegocioException ne) {
            return ne;
        }
        audit.error("Backend no disponible: {}", e.toString());
        return NegocioException.noDisponible("Servicio no disponible. No se realizo ninguna operacion.");
    }
}
