package cl.bancoxyz.pagos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud generica. Segun el endpoint se exige:
 *  transferencia: origen + destino | deposito: destino | retiro y pago de servicio: origen.
 * El cliente se deduce de la cuenta (no se confia en un clienteId enviado por el canal).
 */
public record PagoRequest(
        Long cuentaOrigenId,
        Long cuentaDestinoId,
        @NotNull @DecimalMin(value = "1") @Digits(integer = 16, fraction = 2) BigDecimal monto,
        @Size(max = 200) String descripcion,
        @Size(max = 20) String canal) {
}
