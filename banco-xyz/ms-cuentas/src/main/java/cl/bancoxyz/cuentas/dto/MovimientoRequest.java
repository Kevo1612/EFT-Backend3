package cl.bancoxyz.cuentas.dto;

import java.math.BigDecimal;

import cl.bancoxyz.cuentas.domain.MovimientoCuenta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Solicitud de cargo/abono. Solo la usa ms-pagos (rol SERVICIO). */
public record MovimientoRequest(
        @NotBlank @Size(max = 80) String referencia,
        @NotNull MovimientoCuenta.Tipo tipo,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 16, fraction = 2) BigDecimal monto,
        @Size(max = 200) String descripcion) {
}
