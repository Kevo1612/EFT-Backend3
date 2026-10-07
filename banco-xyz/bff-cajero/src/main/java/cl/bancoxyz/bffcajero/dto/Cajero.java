package cl.bancoxyz.bffcajero.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public final class Cajero {

    private Cajero() {
    }

    // ---- hacia el cajero
    public record Saldo(String cuenta, BigDecimal saldoDisponible, Instant consultado) {
    }

    public record Operacion(@NotNull Long cuentaId, @NotNull @Positive BigDecimal monto) {
    }

    public record ComprobanteOperacion(String referencia, String operacion, String estado, BigDecimal monto,
                                       String cuenta, BigDecimal saldoDisponible, String cajero, Instant fecha,
                                       String mensaje) {
    }

    // ---- internos
    public record CuentaDto(Long id, String numeroCuenta, BigDecimal saldo, String estado) {
    }

    public record PagoDto(String referencia, String tipo, String estado, BigDecimal monto, String motivo) {
    }

    public record PagoInterno(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal monto, String descripcion,
                              String canal) {
    }
}
