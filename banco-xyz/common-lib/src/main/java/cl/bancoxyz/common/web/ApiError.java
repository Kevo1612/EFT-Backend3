package cl.bancoxyz.common.web;

import java.time.Instant;
import java.util.List;

/**
 * Formato estandar de error para todas las APIs.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        List<String> detalles) {
}
