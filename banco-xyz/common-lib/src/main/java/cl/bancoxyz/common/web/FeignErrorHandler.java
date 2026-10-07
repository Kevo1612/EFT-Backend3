package cl.bancoxyz.common.web;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * En los BFF: si el microservicio responde 4xx (regla de negocio) se devuelve al frontend
 * el mismo codigo y cuerpo; si responde 5xx o no responde, se devuelve 503 generico
 * (sin filtrar detalles internos).
 * Solo se activa en servicios que tienen Feign en el classpath.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(name = "feign.FeignException")
public class FeignErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(FeignErrorHandler.class);

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> feign(FeignException ex, HttpServletRequest req) {
        int status = ex.status();
        if (status >= 400 && status < 500) {
            String body = ex.contentUTF8();
            if (body != null && !body.isBlank()) {
                return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return ResponseEntity.status(status).body(new ApiError(Instant.now(), status,
                    HttpStatus.valueOf(status).getReasonPhrase(), "Solicitud rechazada", req.getRequestURI(), List.of()));
        }
        log.warn("Servicio interno fallo en {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(Instant.now(), 503,
                "Service Unavailable", "Servicio temporalmente no disponible", req.getRequestURI(), List.of()));
    }
}
