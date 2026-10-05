package org.banksolution.servicesecurity.feign;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.banksolution.servicesecurity.probe.ProbeClient;
import org.banksolution.servicesecurity.probe.ServiceSecurityTestApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.servicesecurity.testing.ServiceTokenStubs.SERVICE_TOKEN_PATH;
import static org.banksolution.servicesecurity.testing.ServiceTokenStubs.STUBBED_SERVICE_TOKEN;
import static org.banksolution.servicesecurity.testing.ServiceTokenStubs.stubServiceTokenEndpoint;

@SpringBootTest(classes = ServiceSecurityTestApplication.class)
class ServiceTokenFeignConfigurationTest {

    private static final WireMockServer WIRE_MOCK_SERVER = startWireMockServer();

    @Autowired
    private ProbeClient probeClient;

    @DynamicPropertySource
    static void pointAtWireMock(DynamicPropertyRegistry registry) {
        registry.add("probe.url", WIRE_MOCK_SERVER::baseUrl);
        registry.add("spring.security.oauth2.client.provider.bank-internal.token-uri",
                () -> WIRE_MOCK_SERVER.baseUrl() + SERVICE_TOKEN_PATH);
    }

    @AfterAll
    static void stopWireMockServer() {
        WIRE_MOCK_SERVER.stop();
    }

    @Test
    void shouldCallTheDownstreamServiceWithAClientCredentialsTokenAndReuseIt() {
        stubServiceTokenEndpoint(WIRE_MOCK_SERVER);
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/downstream")).willReturn(aResponse().withBody("ok")));

        assertThat(probeClient.callDownstream()).isEqualTo("ok");
        assertThat(probeClient.callDownstream()).isEqualTo("ok");

        WIRE_MOCK_SERVER.verify(2, getRequestedFor(urlEqualTo("/downstream"))
                .withHeader("Authorization", equalTo("Bearer " + STUBBED_SERVICE_TOKEN)));
        WIRE_MOCK_SERVER.verify(1, postRequestedFor(urlEqualTo(SERVICE_TOKEN_PATH))
                .withRequestBody(containing("grant_type=client_credentials")));
    }

    @SuppressWarnings({"resource", "java:S2095"})
    private static WireMockServer startWireMockServer() {
        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        return wireMockServer;
    }
}
