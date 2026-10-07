package cl.bancoxyz.pagos.web;

import java.util.List;

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

import cl.bancoxyz.pagos.domain.Pago;
import cl.bancoxyz.pagos.domain.Pago.Tipo;
import cl.bancoxyz.pagos.dto.PagoRequest;
import cl.bancoxyz.pagos.dto.PagoResponse;
import cl.bancoxyz.pagos.service.PagoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasAnyRole('CLIENTE','ADMIN','SERVICIO')")
public class PagoController {

    private static final String IDEMPOTENCY = "Idempotency-Key";

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    @PostMapping("/transferencias")
    public ResponseEntity<PagoResponse> transferir(@Valid @RequestBody PagoRequest req,
                                                   @RequestHeader(value = IDEMPOTENCY, required = false) String key) {
        return responder(service.procesar(Tipo.TRANSFERENCIA, req, key));
    }

    @PostMapping("/depositos")
    public ResponseEntity<PagoResponse> depositar(@Valid @RequestBody PagoRequest req,
                                                  @RequestHeader(value = IDEMPOTENCY, required = false) String key) {
        return responder(service.procesar(Tipo.DEPOSITO, req, key));
    }

    @PostMapping("/retiros")
    public ResponseEntity<PagoResponse> retirar(@Valid @RequestBody PagoRequest req,
                                                @RequestHeader(value = IDEMPOTENCY, required = false) String key) {
        return responder(service.procesar(Tipo.RETIRO, req, key));
    }

    @PostMapping("/servicios")
    public ResponseEntity<PagoResponse> pagarServicio(@Valid @RequestBody PagoRequest req,
                                                      @RequestHeader(value = IDEMPOTENCY, required = false) String key) {
        return responder(service.procesar(Tipo.PAGO_SERVICIO, req, key));
    }

    @GetMapping("/{referencia}")
    public PagoResponse obtener(@PathVariable String referencia) {
        return service.obtener(referencia);
    }

    @GetMapping("/cuenta/{cuentaId}")
    public List<PagoResponse> historial(@PathVariable Long cuentaId, @RequestParam(defaultValue = "20") int limite) {
        return service.historial(cuentaId, limite);
    }

    /** El codigo HTTP refleja el resultado de la saga. */
    private ResponseEntity<PagoResponse> responder(Pago p) {
        HttpStatus status = switch (p.getEstado()) {
            case COMPLETADO -> HttpStatus.CREATED;
            case PENDIENTE_REINTENTO, REQUIERE_REVISION, PROCESANDO -> HttpStatus.ACCEPTED;
            case REVERTIDO -> HttpStatus.CONFLICT;
            case RECHAZADO -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(PagoResponse.de(p));
    }
}
