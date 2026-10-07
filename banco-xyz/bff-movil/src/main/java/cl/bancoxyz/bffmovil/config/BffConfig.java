package cl.bancoxyz.bffmovil.config;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

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
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                .jwtAuthenticationConverter(new KeycloakRealmRoleConverter())));
        return http.build();
    }

    /** Solo tokens emitidos a la app movil (cliente OAuth2 "movil-app", flujo PKCE en produccion). */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${banco.security.jwk-set-uri}") String jwkSetUri,
                                 @Value("${banco.bff.clientes-permitidos:movil-app}") Set<String> permitidos) {
        return JwtDecoderFactory.crear(jwkSetUri, permitidos);
    }

    @Bean
    public BearerTokenRelayInterceptor tokenRelay() {
        return new BearerTokenRelayInterceptor();
    }

    /**
     * ETag: si el contenido no cambio desde la ultima consulta, la app recibe 304 sin cuerpo
     * (ahorro de datos moviles). Se combina con compresion gzip configurada en el servidor.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> bean = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        bean.addUrlPatterns("/movil/*");
        bean.setName("etagFilter");
        return bean;
    }
}
