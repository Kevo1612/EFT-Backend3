package cl.bancoxyz.bffcajero.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bancoxyz.bffcajero.config.TerminalCajeroFilter;
import cl.bancoxyz.bffcajero.dto.Cajero;
import cl.bancoxyz.bffcajero.service.CajeroService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/cajero")
@PreAuthorize("hasRole('CLIENTE')")
public class CajeroController {

    private final CajeroService service;

    public CajeroController(CajeroService service) {
        this.service = service;
    }

    @GetMapping("/cuentas/{id}/saldo")
    public Cajero.Saldo saldo(@PathVariable Long id, @RequestHeader(TerminalCajeroFilter.HEADER) String cajero) {
        return service.saldo(id, cajero);
    }

    /** Idempotency-Key es OBLIGATORIO: el cajero lo genera una vez por operacion y lo repite si reintenta. */
    @PostMapping("/retiros")
    public Cajero.ComprobanteOperacion retirar(@Valid @RequestBody Cajero.Operacion op,
                                               @RequestHeader(TerminalCajeroFilter.HEADER) String cajero,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return service.retirar(op, cajero, idempotencyKey);
    }

    @PostMapping("/depositos")
    public Cajero.ComprobanteOperacion depositar(@Valid @RequestBody Cajero.Operacion op,
                                                 @RequestHeader(TerminalCajeroFilter.HEADER) String cajero,
                                                 @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return service.depositar(op, cajero, idempotencyKey);
    }
}
