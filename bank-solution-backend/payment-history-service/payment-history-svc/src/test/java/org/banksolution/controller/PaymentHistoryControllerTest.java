package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.banksolution.fixtures.PaymentHistoryFixtures.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentHistoryControllerTest extends BaseIntegrationTest {

    private static final String PAYMENT_HISTORY_URL = "/api/v1/payment-history";

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Test
    void shouldReturnAPaymentWithItsMarlAssessmentById() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentHistoryEntity paymentHistoryEntity = createPaymentHistoryEntity(paymentId, CUSTOMER_ID);
        paymentHistoryEntity.setMarlAssessment(createMarlAssessment());
        paymentHistoryRepository.saveAndFlush(paymentHistoryEntity);

        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fraudIndicators[0]").value("NONE"))
                .andExpect(jsonPath("$.marlAssessment.action").value("BLOCK"))
                .andExpect(jsonPath("$.marlAssessment.transactionAgentObservation.featureContributions[0].feature").value("amount"))
                .andExpect(jsonPath("$.completedAt").value(COMPLETED_AT.toString()));
    }

    @Test
    void shouldRejectAMalformedPaymentIdAndAMissingDateWithACustomError() throws Exception {
        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.header").value("VALIDATION ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for paymentId: not-a-uuid"));
        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/date-range").param("startDate", INITIATED_AT.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter: endDate"));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownPayment() throws Exception {
        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPageACustomersHistoryNewestFirst() throws Exception {
        UUID customerId = UUID.randomUUID();
        PaymentHistoryEntity olderPaymentHistoryEntity = paymentHistoryRepository.saveAndFlush(createPaymentHistoryEntity(UUID.randomUUID(), customerId));
        PaymentHistoryEntity newerPaymentHistoryEntity = paymentHistoryRepository.saveAndFlush(createPaymentHistoryEntity(UUID.randomUUID(), customerId));
        paymentHistoryRepository.saveAndFlush(createPaymentHistoryEntity(UUID.randomUUID(), UUID.randomUUID()));

        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/customer/" + customerId).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].paymentId").value(newerPaymentHistoryEntity.getPaymentId().toString()));

        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/customer/" + customerId + "/date-range")
                        .param("startDate", olderPaymentHistoryEntity.getCreatedAt().minusSeconds(60).toString())
                        .param("endDate", newerPaymentHistoryEntity.getCreatedAt().plusSeconds(60).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldFilterByStatusFraudStatusRiskLevelAndDateRange() throws Exception {
        PaymentHistoryEntity blockedPaymentHistoryEntity = createPaymentHistoryEntity(UUID.randomUUID(), UUID.randomUUID());
        blockedPaymentHistoryEntity.setStatus("BLOCKED");
        blockedPaymentHistoryEntity.setFraudStatus("BLOCKED");
        blockedPaymentHistoryEntity.setRiskLevel("CRITICAL");
        paymentHistoryRepository.saveAndFlush(blockedPaymentHistoryEntity);

        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/status/BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("BLOCKED"))));
        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/fraud-status/BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/risk-level/CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].paymentId").value(blockedPaymentHistoryEntity.getPaymentId().toString()));
        mockMvc.perform(get(PAYMENT_HISTORY_URL + "/date-range")
                        .param("startDate", blockedPaymentHistoryEntity.getCreatedAt().minusSeconds(60).toString())
                        .param("endDate", blockedPaymentHistoryEntity.getCreatedAt().plusSeconds(60).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
        mockMvc.perform(get(PAYMENT_HISTORY_URL).param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.size").value(5));
    }
}
