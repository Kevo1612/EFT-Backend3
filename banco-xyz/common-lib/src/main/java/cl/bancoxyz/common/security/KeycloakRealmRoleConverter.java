package cl.bancoxyz.common.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convierte los roles de realm de Keycloak (claim realm_access.roles) en authorities ROLE_*
 * para poder usar hasRole('ADMIN') / @PreAuthorize en los servicios.
 * El nombre del principal es preferred_username (o el client_id en tokens de servicio).
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, extraerAuthorities(jwt), nombre(jwt));
    }

    public static Collection<GrantedAuthority> extraerAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            for (Object rol : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.toString().toUpperCase()));
            }
        }
        String scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            for (String s : scope.split(" ")) {
                if (!s.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                }
            }
        }
        return authorities;
    }

    private static String nombre(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        if (username != null) {
            return username;
        }
        String azp = jwt.getClaimAsString("azp");
        return azp != null ? azp : jwt.getSubject();
    }
}
