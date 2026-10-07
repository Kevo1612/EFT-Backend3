package cl.bancoxyz.clientes.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import cl.bancoxyz.common.event.Topicos;

@Configuration
public class KafkaConfig {

    /**
     * Si un mensaje falla 3 veces (con 1 s de espera) se envia al topico <topico>.DLT
     * para analisis manual, sin bloquear el resto de la particion.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> template) {
        return new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
    }

    @Bean
    public NewTopic topicoTransaccionesDlt() {
        return TopicBuilder.name(Topicos.TRANSACCIONES_COMPLETADAS + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoAlertasDlt() {
        return TopicBuilder.name(Topicos.ALERTAS_SEGURIDAD + ".DLT").partitions(3).replicas(1).build();
    }
}
