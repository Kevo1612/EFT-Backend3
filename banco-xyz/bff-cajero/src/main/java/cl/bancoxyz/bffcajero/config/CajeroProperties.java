package cl.bancoxyz.bffcajero.config;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas del canal cajero (configurables en config-server: bff-cajero.yml).
 *
 * @param terminalesAutorizados ids de cajeros fisicos habilitados (header X-Cajero-Id)
 * @param montoMinimo           retiro minimo
 * @param montoMaximo           retiro maximo por operacion
 * @param multiplo              los billetes disponibles obligan a retirar multiplos de este valor
 */
@ConfigurationProperties(prefix = "banco.cajero")
public record CajeroProperties(Set<String> terminalesAutorizados, BigDecimal montoMinimo, BigDecimal montoMaximo,
                               BigDecimal multiplo) {

    public CajeroProperties {
        terminalesAutorizados = terminalesAutorizados == null ? Set.of() : Set.copyOf(terminalesAutorizados);
        montoMinimo = montoMinimo == null ? new BigDecimal("5000") : montoMinimo;
        montoMaximo = montoMaximo == null ? new BigDecimal("500000") : montoMaximo;
        multiplo = multiplo == null ? new BigDecimal("5000") : multiplo;
    }
}
