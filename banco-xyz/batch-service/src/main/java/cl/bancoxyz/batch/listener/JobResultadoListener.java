package cl.bancoxyz.batch.listener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.bancoxyz.common.event.AlertaSeguridadEvent;
import cl.bancoxyz.common.event.BatchResultadoEvent;
import cl.bancoxyz.common.event.Topicos;

/**
 * Al terminar cada job: deja un resumen en el log, publica el resultado en Kafka y,
 * si mas del 30% de los registros fue rechazado, emite una alerta de calidad de datos.
 * Si Kafka no esta disponible el job NO falla (la publicacion es "best effort").
 */
@Component
public class JobResultadoListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobResultadoListener.class);
    private static final double UMBRAL_CALIDAD = 0.30;

    private final ObjectProvider<KafkaTemplate<String, Object>> kafka;

    public JobResultadoListener(ObjectProvider<KafkaTemplate<String, Object>> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(">>> Inicia {} params={}", jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution ex) {
        long leidos = 0;
        long escritos = 0;
        long omitidos = 0;
        for (StepExecution s : ex.getStepExecutions()) {
            leidos += s.getReadCount();
            escritos += s.getWriteCount();
            omitidos += s.getSkipCount();
        }
        long duracion = duracionMs(ex.getStartTime(), ex.getEndTime());
        String job = ex.getJobInstance().getJobName();
        String semana = ex.getJobParameters().getString("semana");
        log.info("<<< Fin {} [{}] estado={} exit={} leidos={} escritos={} omitidos={} duracion={}ms",
                job, semana, ex.getStatus(), ex.getExitStatus().getExitCode(), leidos, escritos, omitidos, duracion);

        publicar(Topicos.BATCH_RESULTADOS, job, new BatchResultadoEvent(job, semana, ex.getStatus().name(), leidos,
                escritos, omitidos, duracion, Instant.now()));

        if (leidos > 0 && (double) omitidos / leidos > UMBRAL_CALIDAD) {
            publicar(Topicos.ALERTAS_SEGURIDAD, job, new AlertaSeguridadEvent(UUID.randomUUID().toString(),
                    "CALIDAD_DATOS", AlertaSeguridadEvent.Severidad.MEDIA, null, null,
                    "Job %s (%s): %d de %d registros rechazados".formatted(job, semana, omitidos, leidos),
                    "batch-service", Instant.now()));
        }
    }

    private void publicar(String topico, String clave, Object evento) {
        KafkaTemplate<String, Object> template = kafka.getIfAvailable();
        if (template == null) {
            return;
        }
        try {
            template.send(topico, clave, evento).whenComplete((r, e) -> {
                if (e != null) {
                    log.warn("No se pudo publicar en {}: {}", topico, e.getMessage());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Kafka no disponible, se omite publicacion en {}: {}", topico, e.getMessage());
        }
    }

    private static long duracionMs(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) {
            return 0;
        }
        return Duration.between(inicio.atZone(ZoneId.systemDefault()), fin.atZone(ZoneId.systemDefault())).toMillis();
    }
}
