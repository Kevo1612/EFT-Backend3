package cl.bancoxyz.common.security;

import org.springframework.http.HttpHeaders;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Interceptor Feign para los BFF: reenvia el token del usuario final al microservicio
 * (patron "token relay"), de modo que el microservicio aplique sus propias reglas de autorizacion.
 */
public class BearerTokenRelayInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String token = SecurityUtils.tokenActual();
        if (token != null) {
            template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }
}
