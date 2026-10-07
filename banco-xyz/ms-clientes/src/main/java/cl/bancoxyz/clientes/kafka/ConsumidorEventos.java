package cl.bancoxyz.clientes.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.bancoxyz.clientes.domain.Notificacion;
import cl.bancoxyz.clientes.repository.NotificacionRepository;
import cl.bancoxyz.common.event.AlertaSeguridadEvent;
import cl.bancoxyz.common.event.Topicos;
import cl.bancoxyz.common.event.TransaccionEvent;

/**
 * Consumidor en tiempo real: convierte eventos de otros servicios en notificaciones para el cliente.
 * Es idempotente (no duplica si Kafka reentrega el mensaje).
 */
@Component
public class ConsumidorEventos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final NotificacionRepository repo;

    public ConsumidorEventos(NotificacionRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = Topicos.TRANSACCIONES_COMPLETADAS, groupId = "ms-clientes-notificaciones")
    public void onTransaccion(TransaccionEvent e) {
        log.info("Evento transaccion recibido ref={} tipo={} estado={}", e.referencia(), e.tipo(), e.estado());
        String mensaje = switch (e.estado()) {
            case "COMPLETADO" -> "%s por $%s realizado(a) con exito via %s".formatted(
                    e.tipo().toLowerCase(), e.monto().toPlainString(), e.canal());
            case "REVERTIDO" -> "Su %s por $%s no pudo completarse y fue revertido(a)".formatted(
                    e.tipo().toLowerCase(), e.monto().toPlainString());
            default -> "Su %s por $%s fue rechazado(a)".formatted(e.tipo().toLowerCase(), e.monto().toPlainString());
        };
        guardar(new Notificacion(e.clienteId(), "TRANSACCION", null, mensaje, e.referencia()));
    }

    @KafkaListener(topics = Topicos.ALERTAS_SEGURIDAD, groupId = "ms-clientes-notificaciones")
    public void onAlerta(AlertaSeguridadEvent e) {
        log.warn("ALERTA {} [{}] cliente={} cuenta={}: {}", e.tipo(), e.severidad(), e.clienteId(), e.cuentaId(),
                e.descripcion());
        guardar(new Notificacion(e.clienteId(), "ALERTA", e.severidad().name(), e.descripcion(), e.id()));
    }

    private void guardar(Notificacion n) {
        if (repo.existsByReferenciaAndTipo(n.getReferencia(), n.getTipo())) {
            log.debug("Evento {} ya procesado, se ignora (idempotencia)", n.getReferencia());
            return;
        }
        try {
            repo.save(n);
        } catch (DataIntegrityViolationException dup) {
            // Dos instancias procesaron el mismo evento al mismo tiempo: la restriccion unica lo resuelve
            log.debug("Notificacion duplicada {} descartada", n.getReferencia());
        }
    }
}
