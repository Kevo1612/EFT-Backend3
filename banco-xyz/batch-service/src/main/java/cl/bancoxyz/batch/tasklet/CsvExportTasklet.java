package cl.bancoxyz.batch.tasklet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Exporta el resultado de una consulta a un archivo CSV (reemplaza los reportes planos del legacy).
 * Se escribe primero a un archivo temporal y luego se renombra: nunca queda un reporte a medio escribir.
 */
public class CsvExportTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(CsvExportTasklet.class);

    private final JdbcTemplate jdbc;
    private final String sql;
    private final Function<JobParameters, Object[]> parametros;
    private final Function<JobParameters, Path> destino;

    public CsvExportTasklet(JdbcTemplate jdbc, String sql, Function<JobParameters, Object[]> parametros,
                            Function<JobParameters, Path> destino) {
        this.jdbc = jdbc;
        this.sql = sql;
        this.parametros = parametros;
        this.destino = destino;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws IOException {
        JobParameters params = chunkContext.getStepContext().getStepExecution().getJobParameters();
        Path archivo = destino.apply(params);
        Files.createDirectories(archivo.toAbsolutePath().getParent());
        Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");

        int[] filas = {0};
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            jdbc.query(sql, rs -> {
                try {
                    var meta = rs.getMetaData();
                    int columnas = meta.getColumnCount();
                    if (filas[0] == 0) {
                        StringBuilder cab = new StringBuilder();
                        for (int i = 1; i <= columnas; i++) {
                            cab.append(i > 1 ? "," : "").append(meta.getColumnLabel(i));
                        }
                        w.write(cab.toString());
                        w.newLine();
                    }
                    StringBuilder fila = new StringBuilder();
                    for (int i = 1; i <= columnas; i++) {
                        Object v = rs.getObject(i);
                        fila.append(i > 1 ? "," : "").append(v == null ? "" : escapar(v.toString()));
                    }
                    w.write(fila.toString());
                    w.newLine();
                    filas[0]++;
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            }, parametros.apply(params));
        }
        Files.move(tmp, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        contribution.incrementWriteCount(filas[0]);
        log.info("Reporte generado: {} ({} filas)", archivo.toAbsolutePath(), filas[0]);
        return RepeatStatus.FINISHED;
    }

    private static String escapar(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
