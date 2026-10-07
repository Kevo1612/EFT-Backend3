package cl.bancoxyz.cuentas.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import cl.bancoxyz.common.security.ClientCredentialsFeignInterceptor;
import cl.bancoxyz.common.security.JwtDecoderFactory;
import cl.bancoxyz.common.security.KeycloakRealmRoleConverter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .jwtAuthenticationConverter(new KeycloakRealmRoleConverter())));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${banco.security.jwk-set-uri}") String jwkSetUri) {
        return JwtDecoderFactory.crear(jwkSetUri, Set.of());
    }

    /** Token de servicio (client_credentials, cliente "ms-cuentas") para llamar a ms-clientes. */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository repo,
                                                                 OAuth2AuthorizedClientService service) {
        return ClientCredentialsFeignInterceptor.manager(repo, service);
    }

    @Bean
    public ClientCredentialsFeignInterceptor serviceTokenInterceptor(OAuth2AuthorizedClientManager manager) {
        return new ClientCredentialsFeignInterceptor(manager, "keycloak");
    }
}
