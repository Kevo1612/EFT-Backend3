package cl.bancoxyz.bffcajero.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import cl.bancoxyz.bffcajero.dto.Cajero;

public final class Clients {

    private Clients() {
    }

    @FeignClient(name = "ms-cuentas", path = "/api/cuentas")
    public interface CuentasClient {
        @GetMapping("/{id}")
        Cajero.CuentaDto obtener(@PathVariable("id") Long id);
    }

    @FeignClient(name = "ms-pagos", path = "/api/pagos")
    public interface PagosClient {
        @PostMapping("/retiros")
        Cajero.PagoDto retirar(@RequestBody Cajero.PagoInterno req,
                               @RequestHeader("Idempotency-Key") String idempotencyKey);

        @PostMapping("/depositos")
        Cajero.PagoDto depositar(@RequestBody Cajero.PagoInterno req,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey);
    }
}
