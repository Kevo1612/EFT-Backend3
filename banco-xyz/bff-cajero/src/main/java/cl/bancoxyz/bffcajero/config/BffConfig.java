package cl.bancoxyz.bffcajero.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import cl.bancoxyz.common.security.BearerTokenRelayInterceptor;
import cl.bancoxyz.common.security.JwtDecoderFactory;
import cl.bancoxyz.common.security.KeycloakRealmRoleConverter;

@Configuration
@EnableMethodSecurity
public class BffConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CajeroProperties props) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // El terminal se valida ANTES que el token: un equipo no registrado ni siquiera llega a autenticar
            .addFilterBefore(new TerminalCajeroFilter(props), BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/prometheus", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .jwtAuthenticationConverter(new KeycloakRealmRoleConverter())));
        return http.build();
    }

    /** Solo tokens del cliente OAuth2 "cajero-app" (vida util de 2 minutos, configurada en Keycloak). */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${banco.security.jwk-set-uri}") String jwkSetUri,
                                 @Value("${banco.bff.clientes-permitidos:cajero-app}") Set<String> permitidos) {
        return JwtDecoderFactory.crear(jwkSetUri, permitidos);
    }

    @Bean
    public BearerTokenRelayInterceptor tokenRelay() {
        return new BearerTokenRelayInterceptor();
    }
}
