package cl.bancoxyz.batch.config;

import java.io.FileNotFoundException;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import cl.bancoxyz.batch.listener.RechazoListener;
import cl.bancoxyz.batch.model.RegistroCsv;
import cl.bancoxyz.batch.util.InvalidRecordException;

/**
 * Infraestructura comun a los 3 jobs:
 *  - pool de hilos para steps multihilo / particionados (procesamiento paralelo).
 *  - fabrica de steps de carga "fault tolerant" con la misma politica de errores:
 *      RETRY  (max 3, backoff exponencial 0.5s-5s) para fallos TRANSITORIOS de base de datos
 *             (caida de conexion, deadlock, timeout).
 *      SKIP   para registros invalidos (dato malo): se omiten, se registran y el job continua,
 *             hasta skipLimit; si se supera, el step falla (el archivo se considera corrupto).
 *      startLimit(3): un step no puede reintentarse indefinidamente.
 */
@Configuration
public class BatchInfraConfig {

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor(BatchProperties props) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(props.hilos());
        ex.setMaxPoolSize(props.hilos() * 2);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("batch-");
        ex.initialize();
        return ex;
    }

    /** Ejecuta los jobs lanzados por API/scheduler sin bloquear el hilo HTTP. */
    @Bean
    public ThreadPoolTaskExecutor jobLauncherExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(3);
        ex.setMaxPoolSize(3);
        ex.setQueueCapacity(20);
        ex.setThreadNamePrefix("job-");
        ex.initialize();
        return ex;
    }

    @Bean
    public PasoCargaFactory pasoCargaFactory(JobRepository jobRepository, PlatformTransactionManager tx,
                                             JdbcTemplate jdbc, BatchProperties props,
                                             TaskExecutor batchTaskExecutor) {
        return new PasoCargaFactory(jobRepository, tx, jdbc, props, batchTaskExecutor);
    }

    public static ExponentialBackOffPolicy backOff() {
        ExponentialBackOffPolicy b = new ExponentialBackOffPolicy();
        b.setInitialInterval(500);
        b.setMultiplier(2.0);
        b.setMaxInterval(5000);
        return b;
    }

    /** Construye steps chunk-oriented multihilo y tolerantes a fallos. */
    public static class PasoCargaFactory {

        private final JobRepository jobRepository;
        private final PlatformTransactionManager tx;
        private final JdbcTemplate jdbc;
        private final BatchProperties props;
        private final TaskExecutor executor;

        public PasoCargaFactory(JobRepository jobRepository, PlatformTransactionManager tx, JdbcTemplate jdbc,
                                BatchProperties props, TaskExecutor executor) {
            this.jobRepository = jobRepository;
            this.tx = tx;
            this.jdbc = jdbc;
            this.props = props;
            this.executor = executor;
        }

        public <I extends RegistroCsv, O> Step crear(String nombre, String archivo, ItemReader<I> reader,
                                                     ItemProcessor<I, O> processor, ItemWriter<O> writer) {
            RechazoListener rechazos = new RechazoListener(jdbc, archivo);
            return new StepBuilder(nombre, jobRepository)
                    .<I, O>chunk(props.chunkSize(), tx)
                    .reader(reader)
                    .processor(processor)
                    .writer(writer)
                    .faultTolerant()
                        .retry(TransientDataAccessException.class)
                        .retry(RecoverableDataAccessException.class)
                        .retry(CannotGetJdbcConnectionException.class)
                        .retryLimit(3)
                        .backOffPolicy(backOff())
                        .skip(InvalidRecordException.class)
                        .skip(FlatFileParseException.class)
                        .noSkip(FileNotFoundException.class)
                        .skipLimit(props.skipLimit())
                        .listener((SkipListener<Object, Object>) rechazos)
                    .listener((StepExecutionListener) rechazos)
                    .taskExecutor(executor)
                    .startLimit(3)
                    .build();
        }
    }
}
