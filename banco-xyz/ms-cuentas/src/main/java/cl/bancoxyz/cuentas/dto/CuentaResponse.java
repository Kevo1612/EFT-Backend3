package cl.bancoxyz.cuentas.dto;

import java.math.BigDecimal;
import java.time.Instant;

import cl.bancoxyz.cuentas.domain.Cuenta;

public record CuentaResponse(
        Long id,
        String numeroCuenta,
        Long clienteId,
        String titularUsername,
        String tipo,
        BigDecimal saldo,
        String estado,
        Instant fechaApertura,
        Instant fechaCierre) {

    public static CuentaResponse de(Cuenta c) {
        return new CuentaResponse(c.getId(), c.getNumeroCuenta(), c.getClienteId(), c.getTitularUsername(), c.getTipo().name(),
                c.getSaldo(), c.getEstado().name(), c.getFechaApertura(), c.getFechaCierre());
    }
}
