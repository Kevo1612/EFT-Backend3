package cl.bancoxyz.batch.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import cl.bancoxyz.batch.config.BatchProperties;
import cl.bancoxyz.common.web.NegocioException;

/**
 * Lanzador de jobs con POLITICA DE REEJECUCION AUTOMATICA:
 * si un job termina FAILED (por ejemplo, la base de datos estuvo caida mas alla de los reintentos
 * a nivel de registro), se vuelve a lanzar con los MISMOS parametros. Spring Batch reconoce la
 * instancia fallida y la REANUDA desde el step que fallo (los steps completados no se repiten).
 * Espera exponencial entre intentos; tras agotar los intentos el job queda FAILED para revision manual.
 */
@Service
public class JobRunnerService {

    private static final Logger log = LoggerFactory.getLogger(JobRunnerService.class);

    private final JobLauncher launcher;
    private final JobExplorer explorer;
    private final BatchProperties props;
    private final Map<String, Job> jobs;

    public JobRunnerService(JobLauncher launcher, JobExplorer explorer, BatchProperties props, List<Job> jobs) {
        this.launcher = launcher;
        this.explorer = explorer;
        this.props = props;
        this.jobs = jobs.stream().collect(Collectors.toMap(Job::getName, Function.identity()));
    }

    public Map<String, Job> jobs() {
        return jobs;
    }

    public Job job(String nombre) {
        Job job = jobs.get(nombre);
        if (job == null) {
            throw NegocioException.noEncontrado("Job desconocido: " + nombre + ". Disponibles: " + jobs.keySet());
        }
        return job;
    }

    @Async("jobLauncherExecutor")
    public void ejecutarConReejecucion(String nombre, JobParameters params) {
        Job job = job(nombre);
        long espera = props.esperaReintentoMs();
        int maxIntentos = 1 + props.reintentosJob();
        for (int intento = 1; intento <= maxIntentos; intento++) {
            try {
                JobExecution ex = launcher.run(job, params);
                if (ex.getStatus() != BatchStatus.FAILED) {
                    log.info("Job {} termino {} (intento {}/{})", nombre, ex.getStatus(), intento, maxIntentos);
                    return;
                }
                log.warn("Job {} FALLO en intento {}/{}: {}", nombre, intento, maxIntentos,
                        ex.getAllFailureExceptions());
            } catch (Exception e) {
                // JobInstanceAlreadyComplete, JobRestartException (startLimit superado), etc.: no se reintenta
                log.error("No se pudo lanzar/reanudar {}: {}", nombre, e.getMessage());
                return;
            }
            if (intento < maxIntentos) {
                log.info("Reejecutando {} en {} ms (reanuda desde el step fallido)", nombre, espera);
                dormir(espera);
                espera *= 2;
            }
        }
        log.error("Job {} agoto {} intentos. Queda FAILED para revision; puede reiniciarse via API.", nombre,
                maxIntentos);
    }

    /**
     * Valida que una ejecucion pueda reiniciarse manualmente. El controlador luego llama a
     * ejecutarConReejecucion con los mismos parametros (a traves del proxy, para que sea @Async).
     */
    public JobExecution ejecucionReiniciable(long ejecucionId) {
        JobExecution ex = explorer.getJobExecution(ejecucionId);
        if (ex == null) {
            throw NegocioException.noEncontrado("Ejecucion " + ejecucionId + " no existe");
        }
        if (ex.getStatus() != BatchStatus.FAILED && ex.getStatus() != BatchStatus.STOPPED) {
            throw NegocioException.conflicto("Solo se pueden reiniciar ejecuciones FAILED o STOPPED (estado actual "
                    + ex.getStatus() + ")");
        }
        return ex;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
