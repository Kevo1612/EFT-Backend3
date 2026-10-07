package cl.bancoxyz.cuentas.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TareasProgramadas {

    private final CuentaService service;

    public TareasProgramadas(CuentaService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${banco.cuentas.validacion-pendientes-ms:60000}")
    public void validarCuentasPendientes() {
        service.validarPendientes();
    }
}
