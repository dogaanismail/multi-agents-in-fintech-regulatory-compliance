package org.banksolution.common;

import org.banksolution.common.initializers.WireMockInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.banksolution.common.initializers.WireMockInitializer.WIRE_MOCK_SERVER;
import static org.banksolution.fixtures.MobileGatewayFixtures.CUSTOMER_IDENTITY_PATH;
import static org.banksolution.fixtures.MobileGatewayFixtures.createAccountResponse;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCallerCustomerResponse;
import static org.banksolution.servicesecurity.testing.ServiceTokenStubs.stubServiceTokenEndpoint;

@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(initializers = WireMockInitializer.class)
public abstract class BaseMobileGatewayTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void resetDownstreamServices() {
        WIRE_MOCK_SERVER.resetAll();
        stubServiceTokenEndpoint(WIRE_MOCK_SERVER);
    }

    protected void stubOnboardedCaller() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo(CUSTOMER_IDENTITY_PATH))
                .willReturn(okJson(objectMapper.writeValueAsString(createCallerCustomerResponse()))));
    }

    protected void stubCallerNotOnboarded() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo(CUSTOMER_IDENTITY_PATH))
                .willReturn(aResponse().withStatus(404)));
    }

    protected void stubAccount(
            UUID accountId,
            UUID ownerCustomerId) {

        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo("/api/v1/accounts/" + accountId))
                .willReturn(okJson(objectMapper.writeValueAsString(createAccountResponse(accountId, ownerCustomerId)))));
    }
}
