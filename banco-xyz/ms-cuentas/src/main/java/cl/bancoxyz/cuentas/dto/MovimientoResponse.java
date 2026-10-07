package cl.bancoxyz.cuentas.dto;

import java.math.BigDecimal;
import java.time.Instant;

import cl.bancoxyz.cuentas.domain.MovimientoCuenta;

public record MovimientoResponse(Long id, Long cuentaId, String referencia, String tipo, BigDecimal monto,
                                 BigDecimal saldoResultante, String descripcion, Instant fecha) {

    public static MovimientoResponse de(MovimientoCuenta m) {
        return new MovimientoResponse(m.getId(), m.getCuentaId(), m.getReferencia(), m.getTipo().name(),
                m.getMonto(), m.getSaldoResultante(), m.getDescripcion(), m.getFecha());
    }
}
