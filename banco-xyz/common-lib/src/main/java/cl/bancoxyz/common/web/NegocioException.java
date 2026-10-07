package cl.bancoxyz.common.web;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con su codigo HTTP asociado (404 no encontrado, 409 saldo insuficiente, etc.).
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static NegocioException noEncontrado(String mensaje) {
        return new NegocioException(HttpStatus.NOT_FOUND, mensaje);
    }

    public static NegocioException conflicto(String mensaje) {
        return new NegocioException(HttpStatus.CONFLICT, mensaje);
    }

    public static NegocioException prohibido(String mensaje) {
        return new NegocioException(HttpStatus.FORBIDDEN, mensaje);
    }

    public static NegocioException invalido(String mensaje) {
        return new NegocioException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public static NegocioException noDisponible(String mensaje) {
        return new NegocioException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
