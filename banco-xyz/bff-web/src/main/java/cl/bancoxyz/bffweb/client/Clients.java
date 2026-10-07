package cl.bancoxyz.bffweb.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import cl.bancoxyz.bffweb.dto.Dtos;

/** Clientes Feign hacia los microservicios (resueltos via Eureka + LoadBalancer). */
public final class Clients {

    private Clients() {
    }

    @FeignClient(name = "ms-clientes", path = "/api/clientes")
    public interface ClientesClient {

        @GetMapping("/me")
        Dtos.Cliente yo();

        @GetMapping("/{id}/notificaciones")
        List<Dtos.Notificacion> notificaciones(@PathVariable("id") Long id, @RequestParam("limite") int limite);

        @GetMapping
        Map<String, Object> listar(@RequestParam("page") int page, @RequestParam("size") int size);
    }

    @FeignClient(name = "ms-cuentas", path = "/api/cuentas")
    public interface CuentasClient {

        @GetMapping("/cliente/{clienteId}")
        List<Dtos.Cuenta> porCliente(@PathVariable("clienteId") Long clienteId);

        @GetMapping("/{id}")
        Dtos.Cuenta obtener(@PathVariable("id") Long id);

        @GetMapping("/{id}/movimientos")
        List<Dtos.Movimiento> movimientos(@PathVariable("id") Long id, @RequestParam("limite") int limite);

        @PostMapping
        Dtos.Cuenta abrir(@RequestBody Dtos.AbrirCuentaInterno req);
    }

    @FeignClient(name = "ms-pagos", path = "/api/pagos")
    public interface PagosClient {

        @PostMapping("/transferencias")
        Dtos.Pago transferir(@RequestBody Dtos.PagoInterno req,
                             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey);

        @PostMapping("/servicios")
        Dtos.Pago pagarServicio(@RequestBody Dtos.PagoInterno req,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey);

        @GetMapping("/cuenta/{cuentaId}")
        List<Dtos.Pago> historial(@PathVariable("cuentaId") Long cuentaId, @RequestParam("limite") int limite);
    }
}
