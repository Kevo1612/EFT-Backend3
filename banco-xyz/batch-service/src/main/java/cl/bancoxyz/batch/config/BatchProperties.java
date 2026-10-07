package cl.bancoxyz.batch.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de los procesos batch (se sobreescriben desde config-server: batch-service.yml).
 *
 * @param inputDir          carpeta base de los CSV (classpath:data o file:/ruta)
 * @param outputDir         carpeta donde se generan los reportes CSV
 * @param chunkSize         registros por transaccion (commit interval)
 * @param hilos             hilos para steps multihilo
 * @param particiones       particiones para el step particionado de estados de cuenta
 * @param skipLimit         maximo de registros invalidos tolerados por step antes de fallar el job
 * @param reintentosJob     reejecuciones automaticas de un job que termina FAILED
 * @param esperaReintentoMs espera inicial entre reejecuciones (se duplica cada vez)
 * @param edadMinima        edad minima valida del titular
 * @param edadMaxima        edad maxima valida del titular
 * @param tasasMensuales    tasa de interes mensual por tipo de producto
 */
@ConfigurationProperties(prefix = "banco.batch")
public record BatchProperties(
        String inputDir,
        String outputDir,
        int chunkSize,
        int hilos,
        int particiones,
        int skipLimit,
        int reintentosJob,
        long esperaReintentoMs,
        int edadMinima,
        int edadMaxima,
        Map<String, BigDecimal> tasasMensuales) {

    public BatchProperties {
        inputDir = inputDir == null ? "classpath:data" : inputDir;
        outputDir = outputDir == null ? "./output" : outputDir;
        chunkSize = chunkSize <= 0 ? 100 : chunkSize;
        hilos = hilos <= 0 ? 4 : hilos;
        particiones = particiones <= 0 ? 4 : particiones;
        skipLimit = skipLimit <= 0 ? 1000 : skipLimit;
        reintentosJob = reintentosJob < 0 ? 0 : reintentosJob;
        esperaReintentoMs = esperaReintentoMs <= 0 ? 10_000 : esperaReintentoMs;
        edadMinima = edadMinima <= 0 ? 18 : edadMinima;
        edadMaxima = edadMaxima <= 0 ? 99 : edadMaxima;
        tasasMensuales = tasasMensuales == null || tasasMensuales.isEmpty()
                ? Map.of("ahorro", new BigDecimal("0.0050"),
                         "prestamo", new BigDecimal("0.0150"),
                         "hipoteca", new BigDecimal("0.0075"))
                : Map.copyOf(tasasMensuales);
    }
}
