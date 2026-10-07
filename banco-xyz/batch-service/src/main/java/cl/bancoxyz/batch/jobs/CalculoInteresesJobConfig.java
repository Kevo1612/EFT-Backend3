package cl.bancoxyz.batch.jobs;

import java.nio.file.Path;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
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
import cl.bancoxyz.batch.model.InteresCalculado;
import cl.bancoxyz.batch.model.InteresCsv;
import cl.bancoxyz.batch.processor.InteresProcessor;
import cl.bancoxyz.batch.reader.CsvReaders;
import cl.bancoxyz.batch.tasklet.CsvExportTasklet;
import cl.bancoxyz.batch.tasklet.SqlTasklet;
import cl.bancoxyz.batch.tasklet.SqlTasklet.Sentencia;

/**
 * JOB 2: Calculo de Intereses Mensuales  (legacy: INTMES.cbl + intmes.sh)
 *
 *  limpieza -> calculo (multihilo, skip/retry) -> deduplicar cuentas -> exportarCsv
 *
 * Parametros: semana (carpeta de datos) y periodo (yyyy-MM) al que corresponde el calculo.
 */
@Configuration
public class CalculoInteresesJobConfig {

    public static final String JOB = "calculoInteresesJob";
    private static final String ARCHIVO = "intereses_trimestrales.csv";

    @Bean
    public Job calculoInteresesJob(JobRepository repo, JobResultadoListener resultado,
                                   Step limpiezaInteresesStep, Step calculoInteresesStep,
                                   Step deduplicarInteresesStep, Step exportarInteresesStep) {
        return new JobBuilder(JOB, repo)
                .listener(resultado)
                .start(limpiezaInteresesStep)
                .next(calculoInteresesStep)
                    .on("FAILED").fail()
                .from(calculoInteresesStep)
                    .on("*").to(deduplicarInteresesStep)
                .next(exportarInteresesStep)
                .end()
                .build();
    }

    @Bean
    public Step limpiezaInteresesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc) {
        return new StepBuilder("limpiezaInteresesStep", repo)
                .tasklet(new SqlTasklet(jdbc, List.of(
                        new Sentencia("DELETE FROM intereses_calculados WHERE semana = ? AND periodo = ?",
                                CalculoInteresesJobConfig::semanaPeriodo),
                        new Sentencia("DELETE FROM registros_rechazados WHERE job = '" + JOB + "' AND semana = ?",
                                p -> new Object[]{p.getString("semana")}))), tx)
                .build();
    }

    @Bean
    public Step calculoInteresesStep(PasoCargaFactory pasos,
                                     SynchronizedItemStreamReader<InteresCsv> interesesReader,
                                     InteresProcessor interesesProcessor,
                                     JdbcBatchItemWriter<InteresCalculado> interesesWriter) {
        return pasos.crear("calculoInteresesStep", ARCHIVO, interesesReader, interesesProcessor, interesesWriter);
    }

    /**
     * Registros duplicados del legacy: misma cuenta repetida -> se conserva la primera ocurrencia
     * (menor linea) y las demas se mueven a registros_rechazados. Ademas se marca como
     * POSIBLE_DUPLICADO_TITULAR cuando dos cuentas distintas tienen identicos nombre/saldo/edad/tipo
     * (ej. cuentas 101 y 106 de John Doe en semana_1) para revision manual: no se elimina porque
     * podria ser un titular con dos productos.
     */
    @Bean
    public Step deduplicarInteresesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc) {
        String rechazar = """
                INSERT INTO registros_rechazados (job, semana, archivo, linea, contenido, motivo)
                SELECT '%s', semana, '%s', linea,
                       cuenta_id || ',' || nombre || ',' || saldo || ',' || edad || ',' || tipo,
                       'DUPLICADO: cuenta_id repetido, se conserva la primera ocurrencia'
                  FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY cuenta_id ORDER BY linea) AS rn
                          FROM intereses_calculados WHERE semana = ? AND periodo = ?) d
                 WHERE d.rn > 1
                """.formatted(JOB, ARCHIVO);
        String eliminar = """
                DELETE FROM intereses_calculados t
                 USING (SELECT linea, ROW_NUMBER() OVER (PARTITION BY cuenta_id ORDER BY linea) AS rn
                          FROM intereses_calculados WHERE semana = ? AND periodo = ?) d
                 WHERE t.semana = ? AND t.periodo = ? AND t.linea = d.linea AND d.rn > 1
                """;
        String marcar = """
                UPDATE intereses_calculados t
                   SET observacion = 'POSIBLE_DUPLICADO_TITULAR'
                  FROM (SELECT linea, COUNT(*) OVER (PARTITION BY nombre, saldo, edad, tipo) AS c
                          FROM intereses_calculados WHERE semana = ? AND periodo = ?) d
                 WHERE t.semana = ? AND t.periodo = ? AND t.linea = d.linea AND d.c > 1
                """;
        return new StepBuilder("deduplicarInteresesStep", repo)
                .tasklet(new SqlTasklet(jdbc, List.of(
                        new Sentencia(rechazar, CalculoInteresesJobConfig::semanaPeriodo),
                        new Sentencia(eliminar, CalculoInteresesJobConfig::semanaPeriodoDoble),
                        new Sentencia(marcar, CalculoInteresesJobConfig::semanaPeriodoDoble))), tx)
                .build();
    }

    @Bean
    public Step exportarInteresesStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc,
                                      BatchProperties props) {
        String sql = """
                SELECT cuenta_id, nombre, tipo, saldo, tasa, interes, saldo_final, naturaleza,
                       COALESCE(observacion, '') AS observacion
                  FROM intereses_calculados WHERE semana = ? AND periodo = ? ORDER BY cuenta_id
                """;
        return new StepBuilder("exportarInteresesStep", repo)
                .tasklet(new CsvExportTasklet(jdbc, sql, CalculoInteresesJobConfig::semanaPeriodo,
                        p -> Path.of(props.outputDir(),
                                "intereses_" + p.getString("semana") + "_" + p.getString("periodo") + ".csv")), tx)
                .build();
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<InteresCsv> interesesReader(
            CsvReaders readers, @Value("#{jobParameters['semana']}") String semana) {
        return readers.crear("interesesReader", semana, ARCHIVO,
                new String[]{"cuenta_id", "nombre", "saldo", "edad", "tipo"},
                fs -> new InteresCsv(fs.readRawString("cuenta_id"), fs.readRawString("nombre"),
                        fs.readRawString("saldo"), fs.readRawString("edad"), fs.readRawString("tipo")));
    }

    @Bean
    @StepScope
    public InteresProcessor interesesProcessor(BatchProperties props,
                                               @Value("#{jobParameters['semana']}") String semana,
                                               @Value("#{jobParameters['periodo']}") String periodo) {
        return new InteresProcessor(semana, periodo, props.tasasMensuales(), props.edadMinima(), props.edadMaxima());
    }

    @Bean
    public JdbcBatchItemWriter<InteresCalculado> interesesWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<InteresCalculado>()
                .dataSource(ds)
                .sql("""
                     INSERT INTO intereses_calculados
                            (semana, periodo, linea, cuenta_id, nombre, tipo, edad, saldo, tasa, interes,
                             saldo_final, naturaleza)
                     VALUES (:semana, :periodo, :linea, :cuentaId, :nombre, :tipo, :edad, :saldo, :tasa, :interes,
                             :saldoFinal, :naturaleza)
                     ON CONFLICT (semana, periodo, linea) DO UPDATE SET
                            cuenta_id = EXCLUDED.cuenta_id, nombre = EXCLUDED.nombre, tipo = EXCLUDED.tipo,
                            edad = EXCLUDED.edad, saldo = EXCLUDED.saldo, tasa = EXCLUDED.tasa,
                            interes = EXCLUDED.interes, saldo_final = EXCLUDED.saldo_final,
                            naturaleza = EXCLUDED.naturaleza, observacion = NULL, procesado_en = now()
                     """)
                .itemSqlParameterSourceProvider(i -> new MapSqlParameterSource()
                        .addValue("semana", i.semana())
                        .addValue("periodo", i.periodo())
                        .addValue("linea", i.linea())
                        .addValue("cuentaId", i.cuentaId())
                        .addValue("nombre", i.nombre())
                        .addValue("tipo", i.tipo())
                        .addValue("edad", i.edad())
                        .addValue("saldo", i.saldo())
                        .addValue("tasa", i.tasa())
                        .addValue("interes", i.interes())
                        .addValue("saldoFinal", i.saldoFinal())
                        .addValue("naturaleza", i.naturaleza()))
                .build();
    }

    private static Object[] semanaPeriodo(JobParameters p) {
        return new Object[]{p.getString("semana"), p.getString("periodo")};
    }

    private static Object[] semanaPeriodoDoble(JobParameters p) {
        return new Object[]{p.getString("semana"), p.getString("periodo"), p.getString("semana"),
                p.getString("periodo")};
    }
}
