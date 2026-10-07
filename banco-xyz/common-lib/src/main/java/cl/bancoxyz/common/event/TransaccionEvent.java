package cl.bancoxyz.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Evento publicado por ms-pagos cuando una operacion termina (completada, rechazada o revertida).
 */
public record TransaccionEvent(
        String referencia,
        String tipo,
        String estado,
        Long clienteId,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        BigDecimal monto,
        String canal,
        Instant fecha) {
}
