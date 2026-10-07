package cl.bancoxyz.cuentas.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.bancoxyz.common.security.SecurityUtils;
import cl.bancoxyz.cuentas.dto.AbrirCuentaRequest;
import cl.bancoxyz.cuentas.dto.CuentaResponse;
import cl.bancoxyz.cuentas.dto.MovimientoRequest;
import cl.bancoxyz.cuentas.dto.MovimientoResponse;
import cl.bancoxyz.cuentas.service.CuentaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService service;

    public CuentaController(CuentaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    public CuentaResponse abrir(@Valid @RequestBody AbrirCuentaRequest req) {
        return service.abrir(req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SERVICIO')")
    public CuentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/cliente/{clienteId}")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SERVICIO')")
    public List<CuentaResponse> porCliente(@PathVariable Long clienteId) {
        return service.porCliente(clienteId);
    }

    @GetMapping("/{id}/movimientos")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SERVICIO')")
    public List<MovimientoResponse> movimientos(@PathVariable Long id, @RequestParam(defaultValue = "20") int limite) {
        return service.movimientos(id, limite);
    }

    /** Cargo/abono: SOLO ms-pagos (token de servicio con rol SERVICIO) puede mover dinero. */
    @PostMapping("/{id}/movimientos")
    @PreAuthorize("hasRole('SERVICIO')")
    public MovimientoResponse aplicarMovimiento(@PathVariable Long id, @Valid @RequestBody MovimientoRequest req) {
        return service.aplicarMovimiento(id, req);
    }

    @PatchMapping("/{id}/bloquear")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICIO')")
    public CuentaResponse bloquear(@PathVariable Long id) {
        return service.bloquear(id, "Bloqueo manual por " + SecurityUtils.usuarioActual());
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public CuentaResponse desbloquear(@PathVariable Long id) {
        return service.desbloquear(id);
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('CLIENTE','ADMIN')")
    public CuentaResponse cerrar(@PathVariable Long id) {
        return service.cerrar(id);
    }
}
