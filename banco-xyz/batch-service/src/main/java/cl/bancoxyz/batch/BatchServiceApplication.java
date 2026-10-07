package cl.bancoxyz.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Servicio de procesos batch. NO usa @EnableBatchProcessing para conservar la autoconfiguracion
 * de Spring Boot (JobRepository en PostgreSQL, JobLauncher, JobExplorer).
 */
@SpringBootApplication(scanBasePackages = "cl.bancoxyz")
@ConfigurationPropertiesScan
@EnableAsync
public class BatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchServiceApplication.class, args);
    }
}
