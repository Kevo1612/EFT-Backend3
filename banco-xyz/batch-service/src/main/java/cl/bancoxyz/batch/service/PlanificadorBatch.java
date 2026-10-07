package cl.bancoxyz.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import cl.bancoxyz.batch.jobs.CalculoInteresesJobConfig;
import cl.bancoxyz.batch.jobs.EstadosCuentaJobConfig;
import cl.bancoxyz.batch.jobs.ReporteTransaccionesJobConfig;

/**
 * Reemplaza el crontab del mainframe. Se activa con banco.batch.scheduler.habilitado=true.
 * En un despliegue con varias replicas se recomienda una sola instancia "scheduler" (o ShedLock);
 * de todas formas el JobRepository impide ejecutar dos veces la misma instancia en paralelo.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "banco.batch.scheduler", name = "habilitado", havingValue = "true")
public class PlanificadorBatch {

    private static final Logger log = LoggerFactory.getLogger(PlanificadorBatch.class);

    private final JobRunnerService runner;
    private final String semana;

    public PlanificadorBatch(JobRunnerService runner, @Value("${banco.batch.scheduler.semana:semana_3}") String semana) {
        this.runner = runner;
        this.semana = semana;
    }

    @Scheduled(cron = "${banco.batch.scheduler.cron-transacciones:0 0 2 * * *}", zone = "America/Santiago")
    public void reporteDiario() {
        lanzar(ReporteTransaccionesJobConfig.JOB);
    }

    @Scheduled(cron = "${banco.batch.scheduler.cron-intereses:0 0 3 1 * *}", zone = "America/Santiago")
    public void interesesMensuales() {
        lanzar(CalculoInteresesJobConfig.JOB);
    }

    @Scheduled(cron = "${banco.batch.scheduler.cron-estados:0 0 4 1 1 *}", zone = "America/Santiago")
    public void estadosAnuales() {
        lanzar(EstadosCuentaJobConfig.JOB);
    }

    private void lanzar(String job) {
        log.info("Scheduler lanza {} ({})", job, semana);
        runner.ejecutarConReejecucion(job, ParametrosJob.crear(semana, null, System.currentTimeMillis()));
    }
}
