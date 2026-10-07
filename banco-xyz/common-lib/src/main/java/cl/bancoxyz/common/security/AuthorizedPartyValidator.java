package cl.bancoxyz.common.security;

import java.util.Set;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Valida que el token haya sido emitido para un cliente OAuth2 concreto (claim "azp").
 * Es lo que hace que cada BFF acepte solo tokens de SU canal:
 * un token de la app movil no sirve contra el BFF de cajeros y viceversa.
 */
public class AuthorizedPartyValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> clientesPermitidos;

    public AuthorizedPartyValidator(Set<String> clientesPermitidos) {
        this.clientesPermitidos = Set.copyOf(clientesPermitidos);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String azp = jwt.getClaimAsString("azp");
        if (azp != null && clientesPermitidos.contains(azp)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "El token fue emitido para el cliente '" + azp + "', no permitido en este canal",
                null));
    }
}
