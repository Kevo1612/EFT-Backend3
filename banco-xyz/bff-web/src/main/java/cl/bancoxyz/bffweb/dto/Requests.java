package cl.bancoxyz.bffweb.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class Requests {

    private Requests() {
    }

    public record Transferencia(@NotNull Long cuentaOrigenId, @NotNull Long cuentaDestinoId,
                                @NotNull @DecimalMin("1") BigDecimal monto, @Size(max = 200) String descripcion) {
    }

    public record PagoServicio(@NotNull Long cuentaOrigenId, @NotNull @DecimalMin("1") BigDecimal monto,
                               @NotNull @Size(max = 60) String servicio, @NotNull @Size(max = 40) String numeroCliente) {
    }

    public record AbrirCuenta(@NotNull @Pattern(regexp = "AHORRO|CORRIENTE|VISTA") String tipo) {
    }
}
