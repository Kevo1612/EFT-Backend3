package cl.bancoxyz.batch.model;

import java.math.BigDecimal;

/** Resultado del calculo de interes mensual de una cuenta. naturaleza: ABONO (ahorro) o CARGO (creditos). */
public record InteresCalculado(
        String semana,
        String periodo,
        int linea,
        long cuentaId,
        String nombre,
        String tipo,
        int edad,
        BigDecimal saldo,
        BigDecimal tasa,
        BigDecimal interes,
        BigDecimal saldoFinal,
        String naturaleza) {
}
