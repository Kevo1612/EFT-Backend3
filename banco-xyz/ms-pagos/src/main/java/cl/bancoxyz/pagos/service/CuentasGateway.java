package cl.bancoxyz.pagos.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.bancoxyz.pagos.client.CuentasClient;
import cl.bancoxyz.pagos.client.CuentasClient.CuentaDto;
import cl.bancoxyz.pagos.client.CuentasClient.MovimientoDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Acceso resiliente a ms-cuentas.
 *  - @Retry: 3 intentos con backoff exponencial ante timeouts / 5xx (seguro porque los
 *    movimientos son idempotentes por referencia).
 *  - @CircuitBreaker: si >50% de las llamadas fallan se abre el circuito 20 s y se responde
 *    inmediatamente con el fallback, protegiendo a ms-cuentas y liberando hilos.
 *  - Fallback: distingue rechazo de negocio (4xx) de indisponibilidad (todo lo demas).
 */
@Service
public class CuentasGateway {

    private static final Logger log = LoggerFactory.getLogger(CuentasGateway.class);

    private final CuentasClient client;
    private final ObjectMapper mapper;

    public CuentasGateway(CuentasClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @CircuitBreaker(name = "cuentas", fallbackMethod = "fallbackCuenta")
    @Retry(name = "cuentas")
    public CuentaDto obtener(Long cuentaId) {
        return client.obtener(cuentaId);
    }

    @CircuitBreaker(name = "cuentas", fallbackMethod = "fallbackMovimiento")
    @Retry(name = "cuentas")
    public MovimientoDto debitar(Long cuentaId, String referencia, BigDecimal monto, String descripcion) {
        return client.aplicarMovimiento(cuentaId, MovimientoDto.debito(referencia, monto, descripcion));
    }

    @CircuitBreaker(name = "cuentas", fallbackMethod = "fallbackMovimiento")
    @Retry(name = "cuentas")
    public MovimientoDto acreditar(Long cuentaId, String referencia, BigDecimal monto, String descripcion) {
        return client.aplicarMovimiento(cuentaId, MovimientoDto.credito(referencia, monto, descripcion));
    }

    @SuppressWarnings("unused")
    private CuentaDto fallbackCuenta(Long cuentaId, Throwable error) {
        throw traducir(error);
    }

    @SuppressWarnings("unused")
    private MovimientoDto fallbackMovimiento(Long cuentaId, String referencia, BigDecimal monto, String descripcion,
                                             Throwable error) {
        throw traducir(error);
    }

    private RuntimeException traducir(Throwable error) {
        if (error instanceof FeignException.FeignClientException fce) {
            return new OperacionRechazadaException(fce.status(), extraerMensaje(fce));
        }
        log.warn("ms-cuentas no disponible: {}", error.toString());
        return new ServicioNoDisponibleException("Servicio de cuentas no disponible", error);
    }

    private String extraerMensaje(FeignException e) {
        try {
            JsonNode body = mapper.readTree(e.contentUTF8());
            if (body.hasNonNull("mensaje")) {
                return body.get("mensaje").asText();
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON
        }
        return "Operacion rechazada por ms-cuentas (HTTP " + e.status() + ")";
    }
}
