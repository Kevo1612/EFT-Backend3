package cl.bancoxyz.batch.model;

import java.math.BigDecimal;

/** Estado de cuenta anual consolidado por cuenta (insumo de auditoria). */
public record EstadoCuentaAnual(
        String semana,
        long cuentaId,
        int anio,
        BigDecimal totalDepositos,
        BigDecimal totalRetiros,
        BigDecimal totalCompras,
        BigDecimal totalPagos,
        BigDecimal saldoNeto,
        int cantidadMovimientos,
        int movimientosObservados) {
}
