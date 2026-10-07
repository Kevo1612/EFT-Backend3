package cl.bancoxyz.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.jdbc.core.JdbcTemplate;

import cl.bancoxyz.batch.model.RegistroCsv;

/**
 * Registra en la tabla registros_rechazados cada linea omitida (skip) con su motivo,
 * para que el area de datos pueda corregirla. Al terminar el step, si hubo omisiones,
 * el ExitStatus pasa a "COMPLETED WITH SKIPS" (visible en el historial de ejecuciones).
 * Se crea una instancia nueva por step (no es un singleton compartido).
 */
public class RechazoListener implements SkipListener<Object, Object>, StepExecutionListener {

    public static final String COMPLETED_WITH_SKIPS = "COMPLETED WITH SKIPS";
    private static final Logger log = LoggerFactory.getLogger(RechazoListener.class);
    private static final String SQL = "INSERT INTO registros_rechazados (job, semana, archivo, linea, contenido, motivo) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;
    private final String archivo;
    private volatile String job;
    private volatile String semana;

    public RechazoListener(JdbcTemplate jdbc, String archivo) {
        this.jdbc = jdbc;
        this.archivo = archivo;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.job = stepExecution.getJobExecution().getJobInstance().getJobName();
        this.semana = stepExecution.getJobParameters().getString("semana");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long omitidos = stepExecution.getSkipCount();
        if (omitidos > 0) {
            log.warn("Step {}: {} registros omitidos (ver tabla registros_rechazados)", stepExecution.getStepName(),
                    omitidos);
            if (ExitStatus.COMPLETED.getExitCode().equals(stepExecution.getExitStatus().getExitCode())) {
                return new ExitStatus(COMPLETED_WITH_SKIPS);
            }
        }
        return stepExecution.getExitStatus();
    }

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException fpe) {
            guardar(fpe.getLineNumber(), fpe.getInput(), "Linea ilegible: " + t.getMessage());
        } else {
            guardar(0, null, "Error de lectura: " + t.getMessage());
        }
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        if (item instanceof RegistroCsv r) {
            guardar(r.getLinea(), r.getRaw(), t.getMessage());
        } else {
            guardar(0, String.valueOf(item), t.getMessage());
        }
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        guardar(0, String.valueOf(item), "Error de escritura: " + t.getMessage());
    }

    private void guardar(int linea, String contenido, String motivo) {
        jdbc.update(SQL, job, semana, archivo, linea, recortar(contenido, 500), recortar(motivo, 300));
    }

    private static String recortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
