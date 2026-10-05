package org.banksolution.servicesecurity.testing;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public final class ServiceTokenStubs {

    public static final String SERVICE_TOKEN_PATH = "/realms/bank-internal/protocol/openid-connect/token";
    public static final String STUBBED_SERVICE_TOKEN = "stubbed-service-token";

    private ServiceTokenStubs() {
    }

    public static void stubServiceTokenEndpoint(WireMockServer wireMockServer) {
        wireMockServer.stubFor(post(urlEqualTo(SERVICE_TOKEN_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"access_token":"%s","token_type":"Bearer","expires_in":300}
                                """.formatted(STUBBED_SERVICE_TOKEN))));
    }
}
