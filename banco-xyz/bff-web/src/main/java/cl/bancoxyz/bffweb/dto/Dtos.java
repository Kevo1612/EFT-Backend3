package cl.bancoxyz.bffweb.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contratos propios del BFF Web. Cada BFF define sus DTO de forma independiente,
 * asi el equipo frontend web puede evolucionar sin afectar a movil ni a cajeros.
 */
public final class Dtos {

    private Dtos() {
    }

    public record Cliente(Long id, String rut, String username, String nombres, String apellidos, String email,
                          String telefono, String direccion, String segmento, boolean activo) {
    }

    public record Cuenta(Long id, String numeroCuenta, Long clienteId, String titularUsername, String tipo,
                         BigDecimal saldo, String estado, Instant fechaApertura) {
    }

    public record Movimiento(Long id, String referencia, String tipo, BigDecimal monto, BigDecimal saldoResultante,
                             String descripcion, Instant fecha) {
    }

    public record Pago(String referencia, String tipo, String estado, BigDecimal monto, Long cuentaOrigenId,
                       Long cuentaDestinoId, String canal, String descripcion, String motivo, Instant fechaCreacion) {
    }

    public record Notificacion(Long id, String tipo, String severidad, String mensaje, Instant fecha, boolean leida) {
    }

    /** Peticion hacia ms-pagos (el BFF agrega el canal). */
    public record PagoInterno(Long cuentaOrigenId, Long cuentaDestinoId, BigDecimal monto, String descripcion,
                              String canal) {
    }

    public record AbrirCuentaInterno(Long clienteId, String tipo) {
    }
}
