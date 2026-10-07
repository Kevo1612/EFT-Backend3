package cl.bancoxyz.common.event;

import java.time.Instant;

/**
 * Alerta de seguridad. La severidad CRITICA provoca el bloqueo automatico de la cuenta en ms-cuentas.
 */
public record AlertaSeguridadEvent(
        String id,
        String tipo,
        Severidad severidad,
        Long clienteId,
        Long cuentaId,
        String descripcion,
        String origen,
        Instant fecha) {

    public enum Severidad { BAJA, MEDIA, ALTA, CRITICA }
}
