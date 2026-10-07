package cl.bancoxyz.bffweb.config;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cl.bancoxyz.common.security.BearerTokenRelayInterceptor;
import cl.bancoxyz.common.security.JwtDecoderFactory;
import cl.bancoxyz.common.security.KeycloakRealmRoleConverter;

@Configuration
@EnableMethodSecurity
public class BffConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .jwtAuthenticationConverter(new KeycloakRealmRoleConverter())));
        return http.build();
    }

    /** Autenticacion especifica del canal: solo se aceptan tokens emitidos al cliente OAuth2 "web-app". */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${banco.security.jwk-set-uri}") String jwkSetUri,
                                 @Value("${banco.bff.clientes-permitidos:web-app}") Set<String> permitidos) {
        return JwtDecoderFactory.crear(jwkSetUri, permitidos);
    }

    /** CORS solo para el dominio del frontend web. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${banco.bff.cors-origins:http://localhost:3000}") java.util.List<String> origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Idempotency-Key"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/web/**", cfg);
        return source;
    }

    /** Reenvia el JWT del usuario a los microservicios. */
    @Bean
    public BearerTokenRelayInterceptor tokenRelay() {
        return new BearerTokenRelayInterceptor();
    }

    /**
     * Pool para llamadas paralelas. DelegatingSecurityContextExecutor copia el usuario autenticado
     * a cada hilo, asi el token relay funciona tambien en las llamadas asincronas.
     */
    @Bean
    public Executor bffExecutor(@Value("${banco.bff.hilos:16}") int hilos) {
        return new DelegatingSecurityContextExecutor(Executors.newFixedThreadPool(hilos));
    }
}
