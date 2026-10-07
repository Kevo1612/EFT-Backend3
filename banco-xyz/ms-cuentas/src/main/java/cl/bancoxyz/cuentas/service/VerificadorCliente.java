package cl.bancoxyz.cuentas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.bancoxyz.common.web.NegocioException;
import cl.bancoxyz.cuentas.client.ClientesClient;
import cl.bancoxyz.cuentas.client.ClientesClient.ClienteDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Consulta a ms-clientes protegida con Resilience4j:
 *  - Retry: reintenta fallos transitorios (timeout, 5xx) con backoff exponencial.
 *  - CircuitBreaker: si ms-clientes falla repetidamente deja de llamarlo por un tiempo.
 *  - Fallback: devuelve null (= "no se pudo verificar") para que la apertura se degrade
 *    a estado PENDIENTE_VALIDACION en vez de fallar.
 * Los 4xx (cliente inexistente) no se reintentan ni abren el circuito (ver ignore-exceptions).
 */
@Service
public class VerificadorCliente {

    private static final Logger log = LoggerFactory.getLogger(VerificadorCliente.class);

    private final ClientesClient clientesClient;

    public VerificadorCliente(ClientesClient clientesClient) {
        this.clientesClient = clientesClient;
    }

    @CircuitBreaker(name = "clientes", fallbackMethod = "fallback")
    @Retry(name = "clientes")
    public ClienteDto obtener(Long clienteId) {
        return clientesClient.obtener(clienteId);
    }

    @SuppressWarnings("unused")
    private ClienteDto fallback(Long clienteId, Throwable error) {
        if (error instanceof FeignException.NotFound) {
            throw NegocioException.noEncontrado("El cliente " + clienteId + " no existe");
        }
        if (error instanceof FeignException.FeignClientException fce) {
            throw new NegocioException(org.springframework.http.HttpStatus.valueOf(fce.status()),
                    "ms-clientes rechazo la consulta del cliente " + clienteId);
        }
        log.warn("ms-clientes no disponible ({}). Se continua en modo degradado.", error.toString());
        return null;
    }
}
