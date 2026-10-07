package cl.bancoxyz.clientes.dto;

import java.time.Instant;

import cl.bancoxyz.clientes.domain.Notificacion;

public record NotificacionResponse(Long id, String tipo, String severidad, String mensaje, String referencia,
                                   Instant fecha, boolean leida) {

    public static NotificacionResponse de(Notificacion n) {
        return new NotificacionResponse(n.getId(), n.getTipo(), n.getSeveridad(), n.getMensaje(),
                n.getReferencia(), n.getFecha(), n.isLeida());
    }
}
