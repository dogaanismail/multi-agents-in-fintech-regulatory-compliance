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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.banksolution.common.initializers.WireMockInitializer.WIRE_MOCK_SERVER;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_ACCOUNT_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.CALLER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.OTHER_ACCOUNT_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.OTHER_CUSTOMER_ID;
import static org.banksolution.fixtures.MobileGatewayFixtures.createCustomerJwt;
import static org.banksolution.fixtures.MobileGatewayFixtures.createMobilePaymentRequest;
import static org.banksolution.fixtures.MobileGatewayFixtures.createPaymentHistoryPage;
import static org.banksolution.fixtures.MobileGatewayFixtures.createPaymentRequestResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends BaseMobileGatewayTest {

    private static final String PAYMENTS = "/api/v1/me/payments";
    private static final String PAYMENT_REQUEST_PATH = "/api/v1/payments/request";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    @Test
    void shouldSubmitAPaymentForTheCallerWithTheirIdempotencyKey() throws Exception {
        stubOnboardedCaller();
        stubAccount(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID);
        WIRE_MOCK_SERVER.stubFor(WireMock.post(urlEqualTo(PAYMENT_REQUEST_PATH))
                .willReturn(okJson(objectMapper.writeValueAsString(createPaymentRequestResponse())).withStatus(201)));

        mockMvc.perform(post(PAYMENTS).with(createCustomerJwt())
                        .header(IDEMPOTENCY_KEY, "pay-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMobilePaymentRequest(CALLER_ACCOUNT_ID))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        WIRE_MOCK_SERVER.verify(postRequestedFor(urlEqualTo(PAYMENT_REQUEST_PATH))
                .withHeader(IDEMPOTENCY_KEY, equalTo("pay-42"))
                .withRequestBody(matchingJsonPath("$.customerId", equalTo(CALLER_CUSTOMER_ID.toString())))
                .withRequestBody(matchingJsonPath("$.paymentType", equalTo("TRANSFER_OUT"))));
    }

    @Test
    void shouldRefuseToPayFromAnAccountOfAnotherCustomerWithoutCallingPaymentService() throws Exception {
        stubOnboardedCaller();
        stubAccount(OTHER_ACCOUNT_ID, OTHER_CUSTOMER_ID);

        mockMvc.perform(post(PAYMENTS).with(createCustomerJwt())
                        .header(IDEMPOTENCY_KEY, "pay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMobilePaymentRequest(OTHER_ACCOUNT_ID))))
                .andExpect(status().isNotFound());

        WIRE_MOCK_SERVER.verify(0, postRequestedFor(urlEqualTo(PAYMENT_REQUEST_PATH)));
    }

    @Test
    void shouldRequireAnIdempotencyKey() throws Exception {
        mockMvc.perform(post(PAYMENTS).with(createCustomerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMobilePaymentRequest(CALLER_ACCOUNT_ID))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPassABusinessRejectionFromPaymentServiceThroughUnchanged() throws Exception {
        stubOnboardedCaller();
        stubAccount(CALLER_ACCOUNT_ID, CALLER_CUSTOMER_ID);
        WIRE_MOCK_SERVER.stubFor(WireMock.post(urlEqualTo(PAYMENT_REQUEST_PATH))
                .willReturn(okJson("{\"message\":\"Insufficient funds\"}").withStatus(422)));

        mockMvc.perform(post(PAYMENTS).with(createCustomerJwt())
                        .header(IDEMPOTENCY_KEY, "pay-43")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createMobilePaymentRequest(CALLER_ACCOUNT_ID))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Insufficient funds"));
    }

    @Test
    void shouldShowCustomerFacingStatusesAndNoRiskDetailsInThePaymentHistory() throws Exception {
        stubOnboardedCaller();
        WIRE_MOCK_SERVER.stubFor(WireMock.get(urlPathEqualTo("/api/v1/payment-history/customer/" + CALLER_CUSTOMER_ID))
                .willReturn(okJson(objectMapper.writeValueAsString(
                        createPaymentHistoryPage("COMPLETED", "BLOCKED", "MANUAL_REVIEW_REQUIRED")))));

        mockMvc.perform(get(PAYMENTS).with(createCustomerJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].status").value("DECLINED"))
                .andExpect(jsonPath("$[2].status").value("PROCESSING"))
                .andExpect(jsonPath("$[0].riskScore").doesNotExist())
                .andExpect(jsonPath("$[0].fraudIndicators").doesNotExist());
    }
}
