package cl.bancoxyz.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages incluye cl.bancoxyz.common (manejador global de errores)
@SpringBootApplication(scanBasePackages = "cl.bancoxyz")
public class MsClientesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsClientesApplication.class, args);
    }
}
