package cl.bancoxyz.pagos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReprocesoPendientes {

    private final PagoService service;

    public ReprocesoPendientes(PagoService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${banco.pagos.reproceso-ms:30000}", initialDelay = 30000)
    public void ejecutar() {
        service.reprocesarPendientes();
    }
}
