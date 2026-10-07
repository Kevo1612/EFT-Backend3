package cl.bancoxyz.clientes.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.bancoxyz.clientes.dto.ActualizarContactoRequest;
import cl.bancoxyz.clientes.dto.ClienteRequest;
import cl.bancoxyz.clientes.dto.ClienteResponse;
import cl.bancoxyz.clientes.dto.NotificacionResponse;
import cl.bancoxyz.clientes.service.ClienteService;
import cl.bancoxyz.common.security.SecurityUtils;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ClienteResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    /** Perfil del usuario autenticado (lo usan los BFF para resolver el cliente a partir del token). */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public ClienteResponse yo() {
        return service.obtenerPorUsername(SecurityUtils.usuarioActual());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SERVICIO')")
    public ClienteResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/usuario/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICIO')")
    public ClienteResponse porUsername(@PathVariable String username) {
        return service.obtenerPorUsername(username);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest req) {
        return service.crear(req);
    }

    @PatchMapping("/{id}/contacto")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    public ClienteResponse actualizarContacto(@PathVariable Long id, @Valid @RequestBody ActualizarContactoRequest req) {
        return service.actualizarContacto(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id);
    }

    @GetMapping("/{id}/notificaciones")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    public List<NotificacionResponse> notificaciones(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "20") int limite) {
        return service.notificaciones(id, limite);
    }
}
