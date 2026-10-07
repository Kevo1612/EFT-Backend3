package cl.bancoxyz.pagos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import cl.bancoxyz.common.event.Topicos;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic topicoTransacciones() {
        return TopicBuilder.name(Topicos.TRANSACCIONES_COMPLETADAS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic topicoAlertas() {
        return TopicBuilder.name(Topicos.ALERTAS_SEGURIDAD).partitions(3).replicas(1).build();
    }
}
