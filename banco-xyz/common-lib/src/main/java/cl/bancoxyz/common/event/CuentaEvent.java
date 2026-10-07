package cl.bancoxyz.common.event;

import java.time.Instant;

/**
 * Cambios de estado de una cuenta (ABIERTA, BLOQUEADA, DESBLOQUEADA, CERRADA).
 */
public record CuentaEvent(
        Long cuentaId,
        Long clienteId,
        String numeroCuenta,
        String evento,
        String detalle,
        Instant fecha) {
}
