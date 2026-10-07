package cl.bancoxyz.bffweb.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.bancoxyz.bffweb.client.Clients.ClientesClient;
import cl.bancoxyz.bffweb.client.Clients.CuentasClient;
import cl.bancoxyz.bffweb.client.Clients.PagosClient;
import cl.bancoxyz.bffweb.dto.Dtos;
import cl.bancoxyz.common.web.NegocioException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Llamadas resilientes a los microservicios.
 * Politica del canal Web:
 *  - Datos esenciales (perfil, cuentas): si fallan -> 503 (no se puede mostrar la pantalla).
 *  - Datos secundarios (historial, notificaciones): si fallan -> null y la pantalla se muestra
 *    parcialmente con una advertencia (degradacion elegante).
 *  - Escrituras: sin reintento automatico (lo controla el Idempotency-Key del frontend).
 */
@Service
public class Backend {

    private static final Logger log = LoggerFactory.getLogger(Backend.class);

    private final ClientesClient clientes;
    private final CuentasClient cuentas;
    private final PagosClient pagos;

    public Backend(ClientesClient clientes, CuentasClient cuentas, PagosClient pagos) {
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.pagos = pagos;
    }

    // ---------------------------------------------------------------- esenciales

    @CircuitBreaker(name = "ms-clientes", fallbackMethod = "perfilFallback")
    @Retry(name = "lecturas")
    public Dtos.Cliente perfil() {
        return clientes.yo();
    }

    @CircuitBreaker(name = "ms-cuentas", fallbackMethod = "cuentasFallback")
    @Retry(name = "lecturas")
    public List<Dtos.Cuenta> cuentas(Long clienteId) {
        return cuentas.porCliente(clienteId);
    }

    @CircuitBreaker(name = "ms-cuentas", fallbackMethod = "cuentaFallback")
    @Retry(name = "lecturas")
    public Dtos.Cuenta cuenta(Long cuentaId) {
        return cuentas.obtener(cuentaId);
    }

    @CircuitBreaker(name = "ms-clientes", fallbackMethod = "listarFallback")
    public Map<String, Object> listarClientes(int page, int size) {
        return clientes.listar(page, size);
    }

    // ---------------------------------------------------------------- secundarios (degradables)

    @CircuitBreaker(name = "ms-pagos", fallbackMethod = "historialFallback")
    @Retry(name = "lecturas")
    public List<Dtos.Pago> historial(Long cuentaId, int limite) {
        return pagos.historial(cuentaId, limite);
    }

    @CircuitBreaker(name = "ms-cuentas", fallbackMethod = "movimientosFallback")
    @Retry(name = "lecturas")
    public List<Dtos.Movimiento> movimientos(Long cuentaId, int limite) {
        return cuentas.movimientos(cuentaId, limite);
    }

    @CircuitBreaker(name = "ms-clientes", fallbackMethod = "notificacionesFallback")
    @Retry(name = "lecturas")
    public List<Dtos.Notificacion> notificaciones(Long clienteId, int limite) {
        return clientes.notificaciones(clienteId, limite);
    }

    // ---------------------------------------------------------------- escrituras

    @CircuitBreaker(name = "ms-pagos", fallbackMethod = "escrituraFallback")
    public Dtos.Pago transferir(Dtos.PagoInterno req, String idempotencyKey) {
        return pagos.transferir(req, idempotencyKey);
    }

    @CircuitBreaker(name = "ms-pagos", fallbackMethod = "escrituraFallback")
    public Dtos.Pago pagarServicio(Dtos.PagoInterno req, String idempotencyKey) {
        return pagos.pagarServicio(req, idempotencyKey);
    }

    @CircuitBreaker(name = "ms-cuentas", fallbackMethod = "abrirFallback")
    public Dtos.Cuenta abrirCuenta(Dtos.AbrirCuentaInterno req) {
        return cuentas.abrir(req);
    }

    // ---------------------------------------------------------------- fallbacks

    // Cada metodo tiene su fallback con la MISMA firma + Throwable y el mismo tipo de retorno.

    @SuppressWarnings("unused")
    private Dtos.Cliente perfilFallback(Throwable e) {
        throw esencial(e);
    }

    @SuppressWarnings("unused")
    private List<Dtos.Cuenta> cuentasFallback(Long clienteId, Throwable e) {
        throw esencial(e);
    }

    @SuppressWarnings("unused")
    private Dtos.Cuenta cuentaFallback(Long cuentaId, Throwable e) {
        throw esencial(e);
    }

    @SuppressWarnings("unused")
    private Map<String, Object> listarFallback(int page, int size, Throwable e) {
        throw esencial(e);
    }

    @SuppressWarnings("unused")
    private Dtos.Cuenta abrirFallback(Dtos.AbrirCuentaInterno req, Throwable e) {
        throw esencial(e);
    }

    @SuppressWarnings("unused")
    private List<Dtos.Pago> historialFallback(Long cuentaId, int limite, Throwable e) {
        return secundario(e);
    }

    @SuppressWarnings("unused")
    private List<Dtos.Movimiento> movimientosFallback(Long cuentaId, int limite, Throwable e) {
        return secundario(e);
    }

    @SuppressWarnings("unused")
    private List<Dtos.Notificacion> notificacionesFallback(Long clienteId, int limite, Throwable e) {
        return secundario(e);
    }

    @SuppressWarnings("unused")
    private Dtos.Pago escrituraFallback(Dtos.PagoInterno req, String key, Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            throw fce;
        }
        log.warn("ms-pagos no disponible: {}", e.toString());
        throw NegocioException.noDisponible("No fue posible procesar la operacion. No se realizo ningun cargo.");
    }

    /** Los 4xx de negocio se propagan tal cual; lo demas se traduce a 503. */
    private RuntimeException esencial(Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            return fce;
        }
        if (e instanceof NegocioException ne) {
            return ne;
        }
        log.warn("Servicio esencial no disponible: {}", e.toString());
        return NegocioException.noDisponible("Servicio temporalmente no disponible, intente en unos minutos");
    }

    /** Dato secundario: si falla por indisponibilidad se devuelve null (la vista se degrada). */
    private <T> List<T> secundario(Throwable e) {
        if (e instanceof FeignException.FeignClientException fce) {
            throw fce;
        }
        log.warn("Dato secundario no disponible, se degrada la respuesta: {}", e.toString());
        return null;
    }
}
