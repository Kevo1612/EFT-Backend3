package cl.bancoxyz.batch.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bancoxyz.batch.service.JobRunnerService;
import cl.bancoxyz.batch.service.ParametrosJob;

/** Operacion de los jobs: lanzar, consultar ejecuciones y reiniciar las fallidas. Solo ADMIN. */
@RestController
@RequestMapping("/api/batch")
@PreAuthorize("hasRole('ADMIN')")
public class BatchController {

    private final JobRunnerService runner;
    private final JobExplorer explorer;

    public BatchController(JobRunnerService runner, JobExplorer explorer) {
        this.runner = runner;
        this.explorer = explorer;
    }

    @GetMapping("/jobs")
    public TreeSet<String> jobs() {
        return new TreeSet<>(runner.jobs().keySet());
    }

    /**
     * Lanza un job de forma asincrona. Ejemplo:
     * POST /api/batch/jobs/reporteTransaccionesJob?semana=semana_3
     * POST /api/batch/jobs/calculoInteresesJob?semana=semana_3&periodo=2024-12
     */
    @PostMapping("/jobs/{nombre}")
    public ResponseEntity<Map<String, Object>> lanzar(@PathVariable String nombre,
                                                      @RequestParam(defaultValue = "semana_3") String semana,
                                                      @RequestParam(required = false) String periodo) {
        runner.job(nombre); // valida que exista
        JobParameters params = ParametrosJob.crear(semana, periodo, System.currentTimeMillis());
        runner.ejecutarConReejecucion(nombre, params);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("job", nombre);
        body.put("parametros", valores(params));
        body.put("mensaje", "Job lanzado. Consulte GET /api/batch/jobs/" + nombre + "/ejecuciones");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    @GetMapping("/jobs/{nombre}/ejecuciones")
    public List<Map<String, Object>> ejecuciones(@PathVariable String nombre,
                                                 @RequestParam(defaultValue = "10") int limite) {
        runner.job(nombre);
        List<Map<String, Object>> salida = new ArrayList<>();
        for (JobInstance instancia : explorer.getJobInstances(nombre, 0, Math.min(limite, 50))) {
            for (JobExecution ex : explorer.getJobExecutions(instancia)) {
                salida.add(resumen(ex));
            }
        }
        salida.sort((a, b) -> Long.compare((Long) b.get("ejecucionId"), (Long) a.get("ejecucionId")));
        return salida;
    }

    @PostMapping("/ejecuciones/{id}/reiniciar")
    public ResponseEntity<Map<String, Object>> reiniciar(@PathVariable long id) {
        JobExecution ex = runner.ejecucionReiniciable(id);
        runner.ejecutarConReejecucion(ex.getJobInstance().getJobName(), ex.getJobParameters());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "job", ex.getJobInstance().getJobName(),
                "mensaje", "Reinicio solicitado; se reanudara desde el step fallido"));
    }

    private static Map<String, Object> valores(JobParameters params) {
        Map<String, Object> m = new LinkedHashMap<>();
        params.getParameters().forEach((k, v) -> m.put(k, v.getValue()));
        return m;
    }

    private static Map<String, Object> resumen(JobExecution ex) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ejecucionId", ex.getId());
        m.put("instanciaId", ex.getJobInstance().getInstanceId());
        m.put("estado", ex.getStatus().name());
        m.put("exitCode", ex.getExitStatus().getExitCode());
        m.put("inicio", ex.getStartTime());
        m.put("fin", ex.getEndTime());
        m.put("parametros", valores(ex.getJobParameters()));
        List<Map<String, Object>> pasos = new ArrayList<>();
        for (StepExecution s : ex.getStepExecutions()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("step", s.getStepName());
            p.put("estado", s.getStatus().name());
            p.put("exitCode", s.getExitStatus().getExitCode());
            p.put("leidos", s.getReadCount());
            p.put("filtrados", s.getFilterCount());
            p.put("escritos", s.getWriteCount());
            p.put("omitidos", s.getSkipCount());
            p.put("commits", s.getCommitCount());
            p.put("rollbacks", s.getRollbackCount());
            pasos.add(p);
        }
        m.put("steps", pasos);
        return m;
    }
}
