package cl.bancoxyz.pagos.kafka;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.bancoxyz.common.event.AlertaSeguridadEvent;
import cl.bancoxyz.common.event.AlertaSeguridadEvent.Severidad;
import cl.bancoxyz.common.event.Topicos;
import cl.bancoxyz.common.event.TransaccionEvent;
import cl.bancoxyz.pagos.domain.Pago;

/**
 * Productor Kafka. La clave es el id de la cuenta principal para conservar el orden de
 * los eventos de una misma cuenta (misma particion).
 */
@Component
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final KafkaTemplate<String, Object> kafka;

    public PublicadorEventos(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void transaccion(Pago p) {
        TransaccionEvent e = new TransaccionEvent(p.getReferencia(), p.getTipo().name(), p.getEstado().name(),
                p.getClienteId(), p.getCuentaOrigenId(), p.getCuentaDestinoId(), p.getMonto(), p.getCanal(),
                Instant.now());
        enviar(Topicos.TRANSACCIONES_COMPLETADAS, clave(p), e);
    }

    public void alerta(Pago p, String tipo, Severidad severidad, String descripcion) {
        AlertaSeguridadEvent e = new AlertaSeguridadEvent(UUID.randomUUID().toString(), tipo, severidad,
                p.getClienteId(), p.cuentaPrincipal(), descripcion, "ms-pagos", Instant.now());
        log.warn("Publicando alerta {} [{}] para cuenta {}", tipo, severidad, p.cuentaPrincipal());
        enviar(Topicos.ALERTAS_SEGURIDAD, clave(p), e);
    }

    private void enviar(String topico, String clave, Object evento) {
        kafka.send(topico, clave, evento).whenComplete((r, ex) -> {
            if (ex != null) {
                log.error("Error publicando en {}: {}", topico, ex.getMessage());
            }
        });
    }

    private String clave(Pago p) {
        return String.valueOf(p.cuentaPrincipal());
    }
}
