package cl.bancoxyz.batch.service;

import java.time.YearMonth;
import java.util.regex.Pattern;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import cl.bancoxyz.common.web.NegocioException;

/** Construccion y validacion de parametros de job (evita path traversal en "semana"). */
public final class ParametrosJob {

    private static final Pattern SEMANA = Pattern.compile("^semana_\\d{1,2}$");
    private static final Pattern PERIODO = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private ParametrosJob() {
    }

    /**
     * @param ejecucion valor unico para crear una NUEVA instancia del job (lanzamiento manual o programado).
     *                  Una reejecucion por fallo reutiliza los mismos parametros para REANUDAR la instancia.
     */
    public static JobParameters crear(String semana, String periodo, long ejecucion) {
        if (semana == null || !SEMANA.matcher(semana).matches()) {
            throw NegocioException.invalido("semana debe tener el formato semana_N (ej: semana_3)");
        }
        String p = periodo == null || periodo.isBlank() ? YearMonth.now().toString() : periodo;
        if (!PERIODO.matcher(p).matches()) {
            throw NegocioException.invalido("periodo debe tener el formato yyyy-MM");
        }
        return new JobParametersBuilder()
                .addString("semana", semana)
                .addString("periodo", p)
                .addLong("ejecucion", ejecucion)
                .toJobParameters();
    }
}
