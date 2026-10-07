package cl.bancoxyz.bffmovil.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import cl.bancoxyz.bffmovil.dto.Movil;

public final class Clients {

    private Clients() {
    }

    @FeignClient(name = "ms-clientes", path = "/api/clientes")
    public interface ClientesClient {
        @GetMapping("/me")
        Movil.ClienteDto yo();
    }

    @FeignClient(name = "ms-cuentas", path = "/api/cuentas")
    public interface CuentasClient {
        @GetMapping("/cliente/{clienteId}")
        List<Movil.CuentaDto> porCliente(@PathVariable("clienteId") Long clienteId);

        @GetMapping("/{id}/movimientos")
        List<Movil.MovimientoDto> movimientos(@PathVariable("id") Long id, @RequestParam("limite") int limite);
    }

    @FeignClient(name = "ms-pagos", path = "/api/pagos")
    public interface PagosClient {
        @PostMapping("/transferencias")
        Movil.PagoDto transferir(@RequestBody Movil.PagoInterno req,
                                 @RequestHeader(value = "Idempotency-Key", required = false) String key);
    }
}
