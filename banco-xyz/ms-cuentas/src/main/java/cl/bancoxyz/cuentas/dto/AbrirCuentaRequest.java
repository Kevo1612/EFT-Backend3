package cl.bancoxyz.cuentas.dto;

import cl.bancoxyz.cuentas.domain.Cuenta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AbrirCuentaRequest(@NotNull @Positive Long clienteId, @NotNull Cuenta.Tipo tipo) {
}
