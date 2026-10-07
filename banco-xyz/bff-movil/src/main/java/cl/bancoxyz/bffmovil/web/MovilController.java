package cl.bancoxyz.bffmovil.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bancoxyz.bffmovil.dto.Movil;
import cl.bancoxyz.bffmovil.service.MovilService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/movil")
@PreAuthorize("hasRole('CLIENTE')")
public class MovilController {

    private final MovilService service;

    public MovilController(MovilService service) {
        this.service = service;
    }

    /** Pantalla de inicio de la app: nombre, saldo total y cuentas (enmascaradas). */
    @GetMapping("/resumen")
    public Movil.Resumen resumen() {
        return service.resumen();
    }

    /** Ultimos 10 movimientos en formato compacto: {f: fecha, d: descripcion, m: monto con signo}. */
    @GetMapping("/cuentas/{id}/movimientos")
    public List<Movil.MovCorto> movimientos(@PathVariable Long id) {
        return service.movimientos(id);
    }

    @PostMapping("/transferencias")
    public Movil.Comprobante transferir(@Valid @RequestBody Movil.Transferencia req,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        return service.transferir(req, key);
    }
}
