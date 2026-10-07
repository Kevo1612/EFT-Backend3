package cl.bancoxyz.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;

import cl.bancoxyz.pagos.domain.Pago;

public record PagoResponse(
        String referencia,
        String tipo,
        String estado,
        BigDecimal monto,
        Long clienteId,
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        String canal,
        String descripcion,
        String motivo,
        int intentos,
        Instant fechaCreacion,
        Instant fechaActualizacion) {

    public static PagoResponse de(Pago p) {
        return new PagoResponse(p.getReferencia(), p.getTipo().name(), p.getEstado().name(), p.getMonto(),
                p.getClienteId(), p.getCuentaOrigenId(), p.getCuentaDestinoId(), p.getCanal(), p.getDescripcion(),
                p.getMotivo(), p.getIntentos(), p.getFechaCreacion(), p.getFechaActualizacion());
    }
}
