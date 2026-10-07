package cl.bancoxyz.common.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Interceptor Feign para llamadas servicio-a-servicio: obtiene (y cachea hasta que expire)
 * un token OAuth2 con grant client_credentials y lo agrega a cada llamada.
 * Funciona tambien fuera de una peticion HTTP (por ejemplo en tareas @Scheduled).
 */
public class ClientCredentialsFeignInterceptor implements RequestInterceptor {

    private final OAuth2AuthorizedClientManager manager;
    private final String registrationId;

    public ClientCredentialsFeignInterceptor(OAuth2AuthorizedClientManager manager, String registrationId) {
        this.manager = manager;
        this.registrationId = registrationId;
    }

    @Override
    public void apply(RequestTemplate template) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(registrationId)
                .principal(registrationId)
                .build();
        OAuth2AuthorizedClient client = manager.authorize(request);
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("No se pudo obtener token de servicio para " + registrationId);
        }
        template.header(HttpHeaders.AUTHORIZATION, "Bearer " + client.getAccessToken().getTokenValue());
    }

    /** Fabrica el manager de clientes autorizados para grant client_credentials. */
    public static OAuth2AuthorizedClientManager manager(ClientRegistrationRepository repo,
                                                       OAuth2AuthorizedClientService service) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, service);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }
}
