package cl.bancoxyz.batch.jobs;

import java.nio.file.Path;
import java.sql.Types;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import cl.bancoxyz.batch.config.BatchInfraConfig.PasoCargaFactory;
import cl.bancoxyz.batch.config.BatchProperties;
import cl.bancoxyz.batch.listener.JobResultadoListener;
import cl.bancoxyz.batch.model.MovimientoDiarioCsv;
import cl.bancoxyz.batch.model.TransaccionDiaria;
import cl.bancoxyz.batch.processor.MovimientoDiarioProcessor;
import cl.bancoxyz.batch.reader.CsvReaders;
import cl.bancoxyz.batch.tasklet.CsvExportTasklet;
import cl.bancoxyz.batch.tasklet.SqlTasklet;
import cl.bancoxyz.batch.tasklet.SqlTasklet.Sentencia;

/**
 * JOB 1: Reporte de Transacciones Diarias  (legacy: TRXDIA.cbl + trxdia.sh)
 *
 *  limpieza -> carga (multihilo, skip/retry) -> marcarDuplicados -> resumen -> exportarCsv
 *                 \-- FAILED --> fin del job con estado FAILED (reejecutable desde este step)
 */
@Configuration
public class ReporteTransaccionesJobConfig {

    public static final String JOB = "reporteTransaccionesJob";
    private static final String ARCHIVO = "movimientos_financieros_diarios.csv";

    @Bean
    public Job reporteTransaccionesJob(JobRepository repo, JobResultadoListener resultado,
                                       Step limpiezaTransaccionesStep, Step cargaTransaccionesStep,
                                       Step duplicadosTransaccionesStep, Step resumenTransaccionesStep,
                                       Step exportarTransaccionesStep) {
        return new JobBuilder(JOB, repo)
                .listener(resultado)
                .start(limpiezaTransaccionesStep)
                .next(cargaTransaccionesStep)
                    .on("FAILED").fail()
                .from(cargaTransaccionesStep)
                    .on("*").to(duplicadosTransaccionesStep)
                .next(resumenTransaccionesStep)
                .next(exportarTransaccionesStep)
                .end()
                .build();
    }

    // ------------------------------------------------------------------ steps

    /** Borra resultados previos de la misma semana: reejecutar el job no duplica datos (idempotencia). */
    @Bean
    public Step limpiezaTransaccionesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc) {
        return new StepBuilder("limpiezaTransaccionesStep", repo)
                .tasklet(new SqlTasklet(jdbc, List.of(
                        new Sentencia("DELETE FROM transacciones_diarias WHERE semana = ?", p -> semana(p)),
                        new Sentencia("DELETE FROM resumen_transacciones_diarias WHERE semana = ?", p -> semana(p)),
                        new Sentencia("DELETE FROM registros_rechazados WHERE job = '" + JOB + "' AND semana = ?",
                                p -> semana(p)))), tx)
                .build();
    }

    @Bean
    public Step cargaTransaccionesStep(PasoCargaFactory pasos,
                                       SynchronizedItemStreamReader<MovimientoDiarioCsv> transaccionesReader,
                                       MovimientoDiarioProcessor transaccionesProcessor,
                                       JdbcBatchItemWriter<TransaccionDiaria> transaccionesWriter) {
        return pasos.crear("cargaTransaccionesStep", ARCHIVO, transaccionesReader, transaccionesProcessor,
                transaccionesWriter);
    }

    /**
     * Duplicado = misma fecha, monto y tipo que un registro anterior (ej. id 6 y 8 de semana_1).
     * Se conserva el primero (menor numero de linea) y los demas se marcan como anomalia DUPLICADO.
     */
    @Bean
    public Step duplicadosTransaccionesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc) {
        String sql = """
                UPDATE transacciones_diarias t
                   SET anomalia = TRUE,
                       motivo = CASE WHEN t.motivo IS NULL OR t.motivo = '' THEN 'DUPLICADO'
                                     ELSE t.motivo || ';DUPLICADO' END
                  FROM (SELECT linea,
                               ROW_NUMBER() OVER (PARTITION BY fecha, monto, tipo ORDER BY linea) AS rn
                          FROM transacciones_diarias
                         WHERE semana = ? AND monto IS NOT NULL) d
                 WHERE t.semana = ? AND t.linea = d.linea AND d.rn > 1
                """;
        return new StepBuilder("duplicadosTransaccionesStep", repo)
                .tasklet(new SqlTasklet(jdbc, List.of(new Sentencia(sql, p -> new Object[]{s(p), s(p)}))), tx)
                .build();
    }

    /** Resumen por fecha y tipo: cantidad, monto valido (sin anomalias) y cantidad de anomalias. */
    @Bean
    public Step resumenTransaccionesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc) {
        String sql = """
                INSERT INTO resumen_transacciones_diarias
                       (semana, fecha, tipo, cantidad, monto_valido, cantidad_anomalias)
                SELECT semana, fecha, tipo, COUNT(*),
                       COALESCE(SUM(CASE WHEN NOT anomalia THEN monto END), 0),
                       SUM(CASE WHEN anomalia THEN 1 ELSE 0 END)
                  FROM transacciones_diarias
                 WHERE semana = ?
                 GROUP BY semana, fecha, tipo
                """;
        return new StepBuilder("resumenTransaccionesStep", repo)
                .tasklet(new SqlTasklet(jdbc, List.of(new Sentencia(sql, p -> semana(p)))), tx)
                .build();
    }

    @Bean
    public Step exportarTransaccionesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc,
                                          BatchProperties props) {
        String sql = """
                SELECT fecha, tipo, cantidad, monto_valido, cantidad_anomalias
                  FROM resumen_transacciones_diarias WHERE semana = ? ORDER BY fecha, tipo
                """;
        return new StepBuilder("exportarTransaccionesStep", repo)
                .tasklet(new CsvExportTasklet(jdbc, sql, p -> semana(p),
                        p -> Path.of(props.outputDir(), "reporte_transacciones_" + s(p) + ".csv")), tx)
                .build();
    }

    // ------------------------------------------------------------------ reader / processor / writer

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<MovimientoDiarioCsv> transaccionesReader(
            CsvReaders readers, @Value("#{jobParameters['semana']}") String semana) {
        return readers.crear("transaccionesReader", semana, ARCHIVO, new String[]{"id", "fecha", "monto", "tipo"},
                fs -> new MovimientoDiarioCsv(fs.readRawString("id"), fs.readRawString("fecha"),
                        fs.readRawString("monto"), fs.readRawString("tipo")));
    }

    @Bean
    @StepScope
    public MovimientoDiarioProcessor transaccionesProcessor(@Value("#{jobParameters['semana']}") String semana) {
        return new MovimientoDiarioProcessor(semana);
    }

    /** Upsert por (semana, linea): un reintento o reejecucion nunca duplica filas. */
    @Bean
    public JdbcBatchItemWriter<TransaccionDiaria> transaccionesWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<TransaccionDiaria>()
                .dataSource(ds)
                .sql("""
                     INSERT INTO transacciones_diarias
                            (semana, linea, id_origen, fecha, monto, tipo, anomalia, motivo, fecha_normalizada)
                     VALUES (:semana, :linea, :idOrigen, :fecha, :monto, :tipo, :anomalia, :motivo, :fechaNormalizada)
                     ON CONFLICT (semana, linea) DO UPDATE SET
                            id_origen = EXCLUDED.id_origen, fecha = EXCLUDED.fecha, monto = EXCLUDED.monto,
                            tipo = EXCLUDED.tipo, anomalia = EXCLUDED.anomalia, motivo = EXCLUDED.motivo,
                            fecha_normalizada = EXCLUDED.fecha_normalizada, procesado_en = now()
                     """)
                .itemSqlParameterSourceProvider(t -> new MapSqlParameterSource()
                        .addValue("semana", t.semana())
                        .addValue("linea", t.linea())
                        .addValue("idOrigen", t.idOrigen())
                        .addValue("fecha", t.fecha())
                        .addValue("monto", t.monto(), Types.NUMERIC)
                        .addValue("tipo", t.tipo())
                        .addValue("anomalia", t.anomalia())
                        .addValue("motivo", t.motivo())
                        .addValue("fechaNormalizada", t.fechaNormalizada()))
                .build();
    }

    // ------------------------------------------------------------------ helpers

    private static String s(org.springframework.batch.core.JobParameters p) {
        return p.getString("semana");
    }

    private static Object[] semana(org.springframework.batch.core.JobParameters p) {
        return new Object[]{s(p)};
    }
}
