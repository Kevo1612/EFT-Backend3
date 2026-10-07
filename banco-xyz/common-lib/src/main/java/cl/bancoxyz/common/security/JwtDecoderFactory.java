package cl.bancoxyz.common.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Construye el JwtDecoder usando el endpoint JWKS de Keycloak.
 * Se valida firma + expiracion y, opcionalmente, el cliente emisor (azp).
 * No se valida el "iss" porque dentro de Docker Keycloak se llama "keycloak:8080"
 * y desde el navegador "localhost:8180" (en la nube ambos serian el mismo dominio y
 * se puede activar la validacion de issuer).
 */
public final class JwtDecoderFactory {

    private JwtDecoderFactory() {
    }

    public static JwtDecoder crear(String jwkSetUri, Set<String> clientesPermitidos) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>();
        validadores.add(JwtValidators.createDefault());
        if (clientesPermitidos != null && !clientesPermitidos.isEmpty()) {
            validadores.add(new AuthorizedPartyValidator(clientesPermitidos));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadores));
        return decoder;
    }
}
