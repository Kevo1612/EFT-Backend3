package cl.bancoxyz.cuentas.kafka;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.bancoxyz.common.event.CuentaEvent;
import cl.bancoxyz.common.event.Topicos;
import cl.bancoxyz.cuentas.domain.Cuenta;

@Component
public class PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventos.class);

    private final KafkaTemplate<String, Object> kafka;

    public PublicadorEventos(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    /** La clave del mensaje es el id de cuenta: todos los eventos de una cuenta van a la misma particion (orden). */
    public void cuenta(Cuenta c, String evento, String detalle) {
        CuentaEvent e = new CuentaEvent(c.getId(), c.getClienteId(), c.getNumeroCuenta(), evento, detalle, Instant.now());
        kafka.send(Topicos.CUENTAS_EVENTOS, String.valueOf(c.getId()), e)
             .whenComplete((r, ex) -> {
                 if (ex != null) {
                     log.error("No se pudo publicar evento {} de cuenta {}", evento, c.getId(), ex);
                 }
             });
    }
}
