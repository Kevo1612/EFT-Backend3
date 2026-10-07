package cl.bancoxyz.bffweb.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bancoxyz.bffweb.dto.Dtos;
import cl.bancoxyz.bffweb.dto.Requests;
import cl.bancoxyz.bffweb.dto.Vistas;
import cl.bancoxyz.bffweb.service.WebService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/web")
public class WebController {

    private final WebService service;

    public WebController(WebService service) {
        this.service = service;
    }

    /** Pantalla principal de banca en linea: perfil, cuentas, saldo total, ultimas operaciones y notificaciones. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CLIENTE')")
    public Vistas.Dashboard dashboard() {
        return service.dashboard();
    }

    @GetMapping("/cuentas/{id}")
    @PreAuthorize("hasRole('CLIENTE')")
    public Vistas.DetalleCuenta detalle(@PathVariable Long id) {
        return service.detalleCuenta(id);
    }

    @PostMapping("/cuentas")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Dtos.Cuenta> abrirCuenta(@Valid @RequestBody Requests.AbrirCuenta req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.abrirCuenta(req));
    }

    @PostMapping("/transferencias")
    @PreAuthorize("hasRole('CLIENTE')")
    public Dtos.Pago transferir(@Valid @RequestBody Requests.Transferencia req,
                                @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return service.transferir(req, key);
    }

    @PostMapping("/pagos-servicios")
    @PreAuthorize("hasRole('CLIENTE')")
    public Dtos.Pago pagarServicio(@Valid @RequestBody Requests.PagoServicio req,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return service.pagarServicio(req, key);
    }

    /** Backoffice: solo ejecutivos/administradores (la web es el unico canal con funciones de administracion). */
    @GetMapping("/admin/clientes")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> clientes(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.listarClientes(page, size);
    }
}
