package cl.bancoxyz.batch.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Movimiento anual normalizado. monto siempre positivo; montoFirmado con el signo segun el tipo
 * (deposito +, retiro/compra/pago -). observaciones registra correcciones aplicadas al dato original.
 */
public record MovimientoAnual(
        String semana,
        int linea,
        long cuentaId,
        LocalDate fecha,
        String transaccion,
        BigDecimal monto,
        BigDecimal montoFirmado,
        String descripcion,
        String observaciones) {
}
