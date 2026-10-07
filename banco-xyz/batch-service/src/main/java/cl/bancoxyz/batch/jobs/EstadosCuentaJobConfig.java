package cl.bancoxyz.batch.jobs;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import cl.bancoxyz.batch.config.BatchInfraConfig;
import cl.bancoxyz.batch.config.BatchInfraConfig.PasoCargaFactory;
import cl.bancoxyz.batch.config.BatchProperties;
import cl.bancoxyz.batch.listener.JobResultadoListener;
import cl.bancoxyz.batch.model.EstadoAnualCsv;
import cl.bancoxyz.batch.model.EstadoCuentaAnual;
import cl.bancoxyz.batch.model.MovimientoAnual;
import cl.bancoxyz.batch.processor.EstadoAnualProcessor;
import cl.bancoxyz.batch.reader.CsvReaders;
import cl.bancoxyz.batch.tasklet.CsvExportTasklet;
import cl.bancoxyz.batch.tasklet.SqlTasklet;
import cl.bancoxyz.batch.tasklet.SqlTasklet.Sentencia;

/**
 * JOB 3: Generacion de Estados de Cuenta Anuales  (legacy: ESTANU.cbl + estanu.sh)
 *
 *  limpieza -> normalizar movimientos (multihilo) -> consolidar por cuenta (PARTICIONADO) -> exportarCsv
 *
 * El step de consolidacion divide las cuentas en N particiones (cuenta_id MOD N) que se procesan
 * en paralelo; cada particion es un step independiente, por lo que si una falla, al reejecutar
 * el job solo se reprocesa esa particion. Este mismo esquema permite escalar a "remote partitioning"
 * (particiones en otras instancias) cuando el volumen lo requiera.
 */
@Configuration
public class EstadosCuentaJobConfig {

    public static final String JOB = "estadosCuentaJob";
    private static final String ARCHIVO = "estados_financieros_anuales.csv";

    @Bean
    public Job estadosCuentaJob(JobRepository repo, JobResultadoListener resultado,
                                Step limpiezaEstadosStep, Step normalizarMovimientosStep,
                                Step consolidarEstadosStep, Step exportarEstadosStep) {
        return new JobBuilder(JOB, repo)
                .listener(resultado)
                .start(limpiezaEstadosStep)
                .next(normalizarMovimientosStep)
                    .on("FAILED").fail()
                .from(normalizarMovimientosStep)
                    .on("*").to(consolidarEstadosStep)
                .next(exportarEstadosStep)
                .end()
                .build();
    }

    @Bean
    public Step limpiezaEstadosStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc) {
        return new StepBuilder("limpiezaEstadosStep", repo)
                .tasklet(new SqlTasklet(jdbc, List.of(
                        new Sentencia("DELETE FROM movimientos_anuales WHERE semana = ?", EstadosCuentaJobConfig::semana),
                        new Sentencia("DELETE FROM estado_cuenta_anual WHERE semana = ?", EstadosCuentaJobConfig::semana),
                        new Sentencia("DELETE FROM registros_rechazados WHERE job = '" + JOB + "' AND semana = ?",
                                EstadosCuentaJobConfig::semana))), tx)
                .build();
    }

    @Bean
    public Step normalizarMovimientosStep(PasoCargaFactory pasos,
                                          SynchronizedItemStreamReader<EstadoAnualCsv> estadosReader,
                                          EstadoAnualProcessor estadosProcessor,
                                          JdbcBatchItemWriter<MovimientoAnual> movimientosAnualesWriter) {
        return pasos.crear("normalizarMovimientosStep", ARCHIVO, estadosReader, estadosProcessor,
                movimientosAnualesWriter);
    }

    // ------------------------------------------------------------------ particionado

    /** Crea gridSize contextos; cada uno procesa las cuentas con cuenta_id MOD gridSize = particion. */
    @Bean
    public Partitioner particionadorCuentas() {
        return gridSize -> {
            Map<String, ExecutionContext> particiones = new HashMap<>();
            for (int i = 0; i < gridSize; i++) {
                ExecutionContext ctx = new ExecutionContext();
                ctx.putInt("particion", i);
                ctx.putInt("totalParticiones", gridSize);
                particiones.put("particion" + i, ctx);
            }
            return particiones;
        };
    }

    @Bean
    public Step consolidarEstadosStep(JobRepository repo, Partitioner particionadorCuentas,
                                      Step consolidarEstadosWorkerStep, TaskExecutor batchTaskExecutor,
                                      BatchProperties props) {
        return new StepBuilder("consolidarEstadosStep", repo)
                .partitioner("consolidarEstadosWorkerStep", particionadorCuentas)
                .step(consolidarEstadosWorkerStep)
                .gridSize(props.particiones())
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step consolidarEstadosWorkerStep(JobRepository repo, PlatformTransactionManager tx,
                                            JdbcCursorItemReader<EstadoCuentaAnual> consolidadoReader,
                                            JdbcBatchItemWriter<EstadoCuentaAnual> estadoCuentaWriter,
                                            BatchProperties props) {
        return new StepBuilder("consolidarEstadosWorkerStep", repo)
                .<EstadoCuentaAnual, EstadoCuentaAnual>chunk(props.chunkSize(), tx)
                .reader(consolidadoReader)
                .writer(estadoCuentaWriter)
                .faultTolerant()
                    .retry(TransientDataAccessException.class)
                    .retryLimit(3)
                    .backOffPolicy(BatchInfraConfig.backOff())
                .build();
    }

    /** La agregacion se hace en la BD (GROUP BY): mucho mas eficiente que sumar fila a fila en Java. */
    @Bean
    @StepScope
    public JdbcCursorItemReader<EstadoCuentaAnual> consolidadoReader(
            DataSource ds,
            @Value("#{jobParameters['semana']}") String semana,
            @Value("#{stepExecutionContext['particion']}") Integer particion,
            @Value("#{stepExecutionContext['totalParticiones']}") Integer total) {
        String sql = """
                SELECT cuenta_id, anio,
                       SUM(CASE WHEN transaccion = 'deposito' THEN monto ELSE 0 END) AS total_depositos,
                       SUM(CASE WHEN transaccion = 'retiro'   THEN monto ELSE 0 END) AS total_retiros,
                       SUM(CASE WHEN transaccion = 'compra'   THEN monto ELSE 0 END) AS total_compras,
                       SUM(CASE WHEN transaccion = 'pago'     THEN monto ELSE 0 END) AS total_pagos,
                       SUM(monto_firmado) AS saldo_neto,
                       COUNT(*) AS cantidad,
                       SUM(CASE WHEN observaciones <> '' THEN 1 ELSE 0 END) AS observados
                  FROM movimientos_anuales
                 WHERE semana = ? AND MOD(cuenta_id, ?) = ?
                 GROUP BY cuenta_id, anio
                 ORDER BY cuenta_id, anio
                """;
        return new JdbcCursorItemReaderBuilder<EstadoCuentaAnual>()
                .name("consolidadoReader-" + particion)
                .dataSource(ds)
                .sql(sql)
                .queryArguments(new Object[]{semana, total, particion})
                .saveState(false)
                .rowMapper((rs, n) -> new EstadoCuentaAnual(semana, rs.getLong("cuenta_id"), rs.getInt("anio"),
                        rs.getBigDecimal("total_depositos"), rs.getBigDecimal("total_retiros"),
                        rs.getBigDecimal("total_compras"), rs.getBigDecimal("total_pagos"),
                        rs.getBigDecimal("saldo_neto"), rs.getInt("cantidad"), rs.getInt("observados")))
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<EstadoCuentaAnual> estadoCuentaWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<EstadoCuentaAnual>()
                .dataSource(ds)
                .sql("""
                     INSERT INTO estado_cuenta_anual
                            (semana, cuenta_id, anio, total_depositos, total_retiros, total_compras, total_pagos,
                             saldo_neto, cantidad_movimientos, movimientos_observados)
                     VALUES (:semana, :cuentaId, :anio, :dep, :ret, :com, :pag, :neto, :cant, :obs)
                     ON CONFLICT (semana, cuenta_id, anio) DO UPDATE SET
                            total_depositos = EXCLUDED.total_depositos, total_retiros = EXCLUDED.total_retiros,
                            total_compras = EXCLUDED.total_compras, total_pagos = EXCLUDED.total_pagos,
                            saldo_neto = EXCLUDED.saldo_neto, cantidad_movimientos = EXCLUDED.cantidad_movimientos,
                            movimientos_observados = EXCLUDED.movimientos_observados, generado_en = now()
                     """)
                .itemSqlParameterSourceProvider(e -> new MapSqlParameterSource()
                        .addValue("semana", e.semana())
                        .addValue("cuentaId", e.cuentaId())
                        .addValue("anio", e.anio())
                        .addValue("dep", e.totalDepositos())
                        .addValue("ret", e.totalRetiros())
                        .addValue("com", e.totalCompras())
                        .addValue("pag", e.totalPagos())
                        .addValue("neto", e.saldoNeto())
                        .addValue("cant", e.cantidadMovimientos())
                        .addValue("obs", e.movimientosObservados()))
                .build();
    }

    @Bean
    public Step exportarEstadosStep(JobRepository repo, PlatformTransactionManager tx, JdbcTemplate jdbc,
                                    BatchProperties props) {
        String sql = """
                SELECT cuenta_id, anio, total_depositos, total_retiros, total_compras, total_pagos, saldo_neto,
                       cantidad_movimientos, movimientos_observados
                  FROM estado_cuenta_anual WHERE semana = ? ORDER BY cuenta_id, anio
                """;
        return new StepBuilder("exportarEstadosStep", repo)
                .tasklet(new CsvExportTasklet(jdbc, sql, EstadosCuentaJobConfig::semana,
                        p -> Path.of(props.outputDir(), "estados_cuenta_" + p.getString("semana") + ".csv")), tx)
                .build();
    }

    // ------------------------------------------------------------------ normalizacion

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<EstadoAnualCsv> estadosReader(
            CsvReaders readers, @Value("#{jobParameters['semana']}") String semana) {
        return readers.crear("estadosReader", semana, ARCHIVO,
                new String[]{"cuenta_id", "fecha", "transaccion", "monto", "descripcion"},
                fs -> new EstadoAnualCsv(fs.readRawString("cuenta_id"), fs.readRawString("fecha"),
                        fs.readRawString("transaccion"), fs.readRawString("monto"), fs.readRawString("descripcion")));
    }

    @Bean
    @StepScope
    public EstadoAnualProcessor estadosProcessor(@Value("#{jobParameters['semana']}") String semana) {
        return new EstadoAnualProcessor(semana);
    }

    @Bean
    public JdbcBatchItemWriter<MovimientoAnual> movimientosAnualesWriter(DataSource ds) {
        return new JdbcBatchItemWriterBuilder<MovimientoAnual>()
                .dataSource(ds)
                .sql("""
                     INSERT INTO movimientos_anuales
                            (semana, linea, cuenta_id, fecha, anio, transaccion, monto, monto_firmado, descripcion,
                             observaciones)
                     VALUES (:semana, :linea, :cuentaId, :fecha, :anio, :transaccion, :monto, :montoFirmado,
                             :descripcion, :observaciones)
                     ON CONFLICT (semana, linea) DO UPDATE SET
                            cuenta_id = EXCLUDED.cuenta_id, fecha = EXCLUDED.fecha, anio = EXCLUDED.anio,
                            transaccion = EXCLUDED.transaccion, monto = EXCLUDED.monto,
                            monto_firmado = EXCLUDED.monto_firmado, descripcion = EXCLUDED.descripcion,
                            observaciones = EXCLUDED.observaciones, procesado_en = now()
                     """)
                .itemSqlParameterSourceProvider(m -> new MapSqlParameterSource()
                        .addValue("semana", m.semana())
                        .addValue("linea", m.linea())
                        .addValue("cuentaId", m.cuentaId())
                        .addValue("fecha", m.fecha())
                        .addValue("anio", m.fecha().getYear())
                        .addValue("transaccion", m.transaccion())
                        .addValue("monto", m.monto())
                        .addValue("montoFirmado", m.montoFirmado())
                        .addValue("descripcion", m.descripcion())
                        .addValue("observaciones", m.observaciones()))
                .build();
    }

    private static Object[] semana(JobParameters p) {
        return new Object[]{p.getString("semana")};
    }
}
