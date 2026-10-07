package cl.bancoxyz.cuentas.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.bancoxyz.common.event.AlertaSeguridadEvent;
import cl.bancoxyz.common.event.Topicos;
import cl.bancoxyz.common.web.NegocioException;
import cl.bancoxyz.cuentas.service.CuentaService;

/**
 * Reaccion automatica en tiempo real: una alerta CRITICA sobre una cuenta la bloquea de inmediato.
 */
@Component
public class ConsumidorAlertas {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorAlertas.class);

    private final CuentaService service;

    public ConsumidorAlertas(CuentaService service) {
        this.service = service;
    }

    @KafkaListener(topics = Topicos.ALERTAS_SEGURIDAD, groupId = "ms-cuentas-bloqueos")
    public void onAlerta(AlertaSeguridadEvent alerta) {
        if (alerta.severidad() != AlertaSeguridadEvent.Severidad.CRITICA || alerta.cuentaId() == null) {
            return;
        }
        try {
            service.bloquear(alerta.cuentaId(), "Bloqueo automatico por alerta " + alerta.tipo() + ": "
                    + alerta.descripcion());
            log.warn("Cuenta {} BLOQUEADA automaticamente por alerta {}", alerta.cuentaId(), alerta.id());
        } catch (NegocioException e) {
            log.warn("Alerta {} referencia cuenta inexistente {}", alerta.id(), alerta.cuentaId());
        }
    }
}
