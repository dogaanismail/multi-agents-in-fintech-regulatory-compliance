package org.banksolution.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.banksolution.common.BaseMobileGatewayTest;
import org.banksolution.model.request.MobileOpenAccountRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.banksolution.common.initializers.WireMockInitializer.WIRE_MOCK_SERVER;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_ACCOUNT_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.OTHER_ACCOUNT_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.OTHER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.createAccountResponse;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCustomerJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerTest extends BaseMobileGatewayTest {

    private static final String ACCOUNTS = "/api/v1/me/accounts";

    @Test
    void shouldListOnlyTheCallersAccounts() throws Exception {
        stubOnboardedCaller();
        WIRE_MOCK_SERVER.stubFor(WireMock.get(urlEqualTo("/api/v1/accounts/customer/" + CALLER_CUSTOMER_ID))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(createAccountResponse(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID))))));

        mockMvc.perform(get(ACCOUNTS).with(createCustomerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(CALLER_ACCOUNT_ID.toString()))
                .andExpect(jsonPath("$[0].wallets[0].availableBalance").value(200.00));
    }

    @Test
    void shouldShowOneOfTheCallersAccounts() throws Exception {
        stubOnboardedCaller();
        stubAccount(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID);

        mockMvc.perform(get(ACCOUNTS + "/" + CALLER_ACCOUNT_ID).with(createCustomerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("GB00BANK0001"));
    }

    @Test
    void shouldHideAnAccountOfAnotherCustomerBehindNotFound() throws Exception {
        stubOnboardedCaller();
        stubAccount(OTHER_ACCOUNT_ID, OTHER_CUSTOMER_ID);

        mockMvc.perform(get(ACCOUNTS + "/" + OTHER_ACCOUNT_ID).with(createCustomerJwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldOpenTheAccountForTheCallerRatherThanAnyCustomerInTheRequest() throws Exception {
        stubOnboardedCaller();
        WIRE_MOCK_SERVER.stubFor(WireMock.post(urlEqualTo("/api/v1/accounts/open-account"))
                .willReturn(okJson(objectMapper.writeValueAsString(createAccountResponse(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID))).withStatus(201)));

        mockMvc.perform(post(ACCOUNTS).with(createCustomerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MobileOpenAccountRequest("CHECKING", "GB", List.of("GBP")))))
                .andExpect(status().isCreated());

        WIRE_MOCK_SERVER.verify(postRequestedFor(urlEqualTo("/api/v1/accounts/open-account"))
                .withRequestBody(matchingJsonPath("$.customerId", equalTo(CALLER_CUSTOMER_ID.toString()))));
    }
}
