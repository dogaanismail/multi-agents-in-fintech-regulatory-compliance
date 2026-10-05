package org.banksolution.api.controller;

import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.OverrideDecisionRequest;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.servicesecurity.Permissions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.banksolution.fixtures.PaymentFixtures.APPROVAL_NOTES;
import static org.banksolution.fixtures.PaymentFixtures.OVERRIDE_REASON;
import static org.banksolution.servicesecurity.testing.ServiceSecurityTestFixtures.createCallerJwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentCommandSecurityTest extends BaseIntegrationTest {

    private static final String PAYMENTS_URL = "/api/v1/payment-engine/payments/";

    @Test
    void shouldRejectAManualReviewWithoutABearerToken() throws Exception {
        mockMvc.perform(post(PAYMENTS_URL + UUID.randomUUID() + "/manual-review/approve")
                        .with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveManualReviewRequest(null, APPROVAL_NOTES))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidAManualReviewToACallerWhoCanOnlyReadPayments() throws Exception {
        mockMvc.perform(post(PAYMENTS_URL + UUID.randomUUID() + "/manual-review/approve")
                        .with(createCallerJwt("viewer", Permissions.PAYMENT_READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveManualReviewRequest(null, APPROVAL_NOTES))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.header").value("AUTH ERROR"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldForbidAnOverrideToACallerWhoCanOnlyReviewPayments() throws Exception {
        mockMvc.perform(post(PAYMENTS_URL + UUID.randomUUID() + "/decision/override")
                        .with(createCallerJwt("junior-officer", Permissions.PAYMENT_REVIEW))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OverrideDecisionRequest(null, OVERRIDE_REASON, true))))
                .andExpect(status().isForbidden());
    }
}
