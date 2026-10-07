package cl.bancoxyz.bffmovil.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contratos del canal movil: nombres cortos, solo campos esenciales, sin nulos
 * (JsonInclude.NON_NULL) -> payloads hasta ~70% mas pequenos que los del BFF web.
 */
public final class Movil {

    private Movil() {
    }

    // ---- respuestas hacia la app

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resumen(String nombre, BigDecimal saldoTotal, List<CuentaCorta> cuentas, Boolean cache,
                          Instant actualizado) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CuentaCorta(Long id, String num, String tipo, BigDecimal saldo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MovCorto(LocalDate f, String d, BigDecimal m) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Comprobante(String ref, String estado, String msg) {
    }

    public record Transferencia(@NotNull Long origen, @NotNull Long destino, @NotNull @DecimalMin("1") BigDecimal monto,
                                @Size(max = 60) String glosa) {
    }

    // ---- DTOs internos (respuesta de microservicios; se ignoran campos extra)

    public record ClienteDto(Long id, String nombres) {
    }

    public record CuentaDto(Long id, String numeroCuenta, String tipo, BigDecimal saldo, String estado) {
    }

    public record MovimientoDto(String tipo, BigDecimal monto, String descripcion, Instant fecha) {
    }

    public record PagoDto(String referencia, String estado, String motivo) {
    }

    public record PagoInterno(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal monto, String descripcion,
                              String canal) {
    }
}
