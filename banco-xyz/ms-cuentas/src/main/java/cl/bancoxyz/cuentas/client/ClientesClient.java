package cl.bancoxyz.cuentas.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo hacia ms-clientes. "ms-clientes" se resuelve en Eureka y
 * Spring Cloud LoadBalancer reparte las llamadas entre todas sus instancias.
 */
@FeignClient(name = "ms-clientes", path = "/api/clientes")
public interface ClientesClient {

    @GetMapping("/{id}")
    ClienteDto obtener(@PathVariable("id") Long id);

    record ClienteDto(Long id, String username, String nombres, String apellidos, boolean activo) {
    }
}
