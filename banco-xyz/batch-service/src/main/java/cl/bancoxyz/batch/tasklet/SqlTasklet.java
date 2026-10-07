package cl.bancoxyz.batch.tasklet;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ejecuta una lista de sentencias SQL dentro de la transaccion del step.
 * Se usa para limpieza (idempotencia), deduplicacion y agregaciones set-based
 * (mucho mas rapidas en la base de datos que fila a fila en Java).
 */
public class SqlTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SqlTasklet.class);

    /** Sentencia + funcion que obtiene sus parametros a partir de los JobParameters. */
    public record Sentencia(String sql, Function<JobParameters, Object[]> parametros) {
    }

    private final JdbcTemplate jdbc;
    private final List<Sentencia> sentencias;

    public SqlTasklet(JdbcTemplate jdbc, List<Sentencia> sentencias) {
        this.jdbc = jdbc;
        this.sentencias = sentencias;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        JobParameters params = chunkContext.getStepContext().getStepExecution().getJobParameters();
        for (Sentencia s : sentencias) {
            int filas = jdbc.update(s.sql(), s.parametros().apply(params));
            contribution.incrementWriteCount(filas);
            log.debug("{} filas afectadas por: {}", filas, s.sql());
        }
        return RepeatStatus.FINISHED;
    }
}
