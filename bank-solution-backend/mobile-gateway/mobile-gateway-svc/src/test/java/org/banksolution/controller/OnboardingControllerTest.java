package org.banksolution.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.banksolution.common.BaseMobileGatewayTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.banksolution.common.initializers.WireMockInitializer.WIRE_MOCK_SERVER;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_EMAIL;
import static org.banksolution.fixtures.MobileGatewayFixtures.CUSTOMER_IDENTITY_PATH;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCallerCustomerResponse;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCustomerJwt;
import static org.banksolution.fixtures.MobileGatewayFixtures.createOnboardingRequest;
import static org.banksolution.servicesecurity.testing.ServiceTokenStubs.STUBBED_SERVICE_TOKEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OnboardingControllerTest extends BaseMobileGatewayTest {

    private static final String ONBOARDING = "/api/v1/me/onboarding";

    @Test
    void shouldOnboardWithTheEmailOfTheLoginUsingTheServiceToken() throws Exception {
        stubCallerNotOnboarded();
        WIRE_MOCK_SERVER.stubFor(WireMock.post(urlEqualTo(CUSTOMER_IDENTITY_PATH))
                .willReturn(okJson(objectMapper.writeValueAsString(createCallerCustomerResponse())).withStatus(201)));

        mockMvc.perform(post(ONBOARDING).with(createCustomerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOnboardingRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CALLER_CUSTOMER_ID.toString()));

        WIRE_MOCK_SERVER.verify(postRequestedFor(urlEqualTo(CUSTOMER_IDENTITY_PATH))
                .withHeader("Authorization", equalTo("Bearer " + STUBBED_SERVICE_TOKEN))
                .withRequestBody(matchingJsonPath("$.email", equalTo(CALLER_EMAIL)))
                .withRequestBody(matchingJsonPath("$.customerType", equalTo("INDIVIDUAL"))));
    }

    @Test
    void shouldReturnTheExistingCustomerWhenOnboardingIsRepeated() throws Exception {
        stubOnboardedCaller();

        mockMvc.perform(post(ONBOARDING).with(createCustomerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOnboardingRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CALLER_CUSTOMER_ID.toString()));

        WIRE_MOCK_SERVER.verify(0, postRequestedFor(urlEqualTo(CUSTOMER_IDENTITY_PATH)));
    }

    @Test
    void shouldRejectIncompletePersonalDetails() throws Exception {
        mockMvc.perform(post(ONBOARDING).with(createCustomerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Ada\"}"))
                .andExpect(status().isBadRequest());
    }
}
