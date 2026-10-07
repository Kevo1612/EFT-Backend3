package cl.bancoxyz.pagos.client;

import java.math.BigDecimal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms-cuentas", path = "/api/cuentas")
public interface CuentasClient {

    @GetMapping("/{id}")
    CuentaDto obtener(@PathVariable("id") Long id);

    @PostMapping("/{id}/movimientos")
    MovimientoDto aplicarMovimiento(@PathVariable("id") Long id, @RequestBody MovimientoDto movimiento);

    record CuentaDto(Long id, String numeroCuenta, Long clienteId, String titularUsername, String tipo,
                     BigDecimal saldo, String estado) {
    }

    record MovimientoDto(String referencia, String tipo, BigDecimal monto, String descripcion,
                         BigDecimal saldoResultante) {

        public static MovimientoDto debito(String ref, BigDecimal monto, String desc) {
            return new MovimientoDto(ref, "DEBITO", monto, desc, null);
        }

        public static MovimientoDto credito(String ref, BigDecimal monto, String desc) {
            return new MovimientoDto(ref, "CREDITO", monto, desc, null);
        }
    }
}
