package cl.bancoxyz.batch.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;

/**
 * Normaliza las fechas del sistema legacy, que llegan en varios formatos:
 * yyyy-MM-dd (estandar), yyyy/MM/dd, dd-MM-yyyy y dd/MM/yyyy.
 * Se usa ResolverStyle.STRICT para rechazar fechas imposibles (ej: 2024-02-30).
 */
public final class FechaParser {

    private static final DateTimeFormatter ISO = formato("uuuu-MM-dd");
    private static final List<DateTimeFormatter> ALTERNATIVOS = List.of(
            formato("uuuu/MM/dd"),
            formato("dd-MM-uuuu"),
            formato("dd/MM/uuuu"));

    private FechaParser() {
    }

    public static Optional<LocalDate> parsear(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        String t = texto.trim();
        Optional<LocalDate> iso = intentar(ISO, t);
        if (iso.isPresent()) {
            return iso;
        }
        for (DateTimeFormatter f : ALTERNATIVOS) {
            Optional<LocalDate> r = intentar(f, t);
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    /** true si la fecha venia en el formato estandar (si no, se marca como normalizada). */
    public static boolean esFormatoEstandar(String texto) {
        return texto != null && intentar(ISO, texto.trim()).isPresent();
    }

    private static Optional<LocalDate> intentar(DateTimeFormatter f, String t) {
        try {
            return Optional.of(LocalDate.parse(t, f));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static DateTimeFormatter formato(String patron) {
        return DateTimeFormatter.ofPattern(patron).withResolverStyle(ResolverStyle.STRICT);
    }
}
