package cl.bancoxyz.gateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Seguridad perimetral (primera barrera):
 *  - /web, /movil, /cajero: requieren un JWT valido; cada BFF valida ademas su canal.
 *  - /api/** (acceso directo a microservicios y batch): solo administradores.
 *    Los clientes finales siempre entran por su BFF.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(ex -> ex
                .pathMatchers("/actuator/**", "/fallback/**").permitAll()
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/api/**").hasRole("ADMIN")
                .pathMatchers("/web/**", "/movil/**", "/cajero/**").authenticated()
                .anyExchange().denyAll())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(
                    j -> new JwtAuthenticationToken(j, roles(j.getClaims()))))));
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(@Value("${banco.security.jwk-set-uri}") String jwkSetUri) {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    private static Collection<GrantedAuthority> roles(Map<String, Object> claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (claims.get("realm_access") instanceof Map<?, ?> realm && realm.get("roles") instanceof Collection<?> roles) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString().toUpperCase())));
        }
        return authorities;
    }
}
