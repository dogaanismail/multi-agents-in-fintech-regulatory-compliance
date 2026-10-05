package org.banksolution.servicesecurity.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

public class ServiceTokenRequestInterceptor implements RequestInterceptor {

    public static final String SERVICE_TOKEN_REGISTRATION_ID = "bank-internal";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public ServiceTokenRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(SERVICE_TOKEN_REGISTRATION_ID)
                .principal(SERVICE_TOKEN_REGISTRATION_ID)
                .build();
        OAuth2AuthorizedClient serviceClient = authorizedClientManager.authorize(authorizeRequest);

        if (serviceClient == null) {
            throw new IllegalStateException("No service token for client registration " + SERVICE_TOKEN_REGISTRATION_ID);
        }

        requestTemplate.header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceClient.getAccessToken().getTokenValue());
    }
}
