package cl.bancoxyz.cuentas.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import cl.bancoxyz.common.event.Topicos;

@Configuration
public class KafkaConfig {

    /** 3 particiones: permite hasta 3 consumidores en paralelo por grupo (escalado horizontal). */
    @Bean
    public NewTopic topicoCuentas() {
        return TopicBuilder.name(Topicos.CUENTAS_EVENTOS).partitions(3).replicas(1).build();
    }
}
