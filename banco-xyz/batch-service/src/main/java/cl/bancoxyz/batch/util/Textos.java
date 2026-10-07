package cl.bancoxyz.batch.util;

import java.math.BigDecimal;

public final class Textos {

    private Textos() {
    }

    public static String limpio(String s) {
        return s == null ? "" : s.trim();
    }

    public static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    public static Long entero(String s, String campo) {
        try {
            return Long.valueOf(limpio(s));
        } catch (NumberFormatException e) {
            throw new InvalidRecordException(campo + " no numerico: '" + limpio(s) + "'");
        }
    }

    public static BigDecimal decimal(String s, String campo) {
        try {
            return new BigDecimal(limpio(s));
        } catch (NumberFormatException e) {
            throw new InvalidRecordException(campo + " no numerico: '" + limpio(s) + "'");
        }
    }
}
