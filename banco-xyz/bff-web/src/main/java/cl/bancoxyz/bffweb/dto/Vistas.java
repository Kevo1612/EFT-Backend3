package cl.bancoxyz.bffweb.dto;

import java.math.BigDecimal;
import java.util.List;

/** Vistas agregadas que entrega el BFF Web (datos completos para pantallas ricas). */
public final class Vistas {

    private Vistas() {
    }

    public record Dashboard(
            Dtos.Cliente cliente,
            List<Dtos.Cuenta> cuentas,
            BigDecimal saldoTotal,
            List<Dtos.Pago> ultimasOperaciones,
            List<Dtos.Notificacion> notificaciones,
            List<String> advertencias) {
    }

    public record DetalleCuenta(
            Dtos.Cuenta cuenta,
            List<Dtos.Movimiento> movimientos,
            List<Dtos.Pago> operaciones,
            List<String> advertencias) {
    }
}
