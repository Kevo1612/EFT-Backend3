package cl.bancoxyz.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Transaccion validada. anomalia=true + motivo cuando el dato es sospechoso (se conserva para el reporte). */
public record TransaccionDiaria(
        String semana,
        int linea,
        long idOrigen,
        LocalDate fecha,
        BigDecimal monto,
        String tipo,
        boolean anomalia,
        String motivo,
        boolean fechaNormalizada) {
}
