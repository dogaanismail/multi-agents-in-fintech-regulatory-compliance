package org.banksolution.api.controller;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.payment.PaymentSnapshotEvent;
import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.InitiatePaymentRequest;
import org.banksolution.api.dto.OverrideDecisionRequest;
import org.banksolution.api.dto.RejectManualReviewRequest;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.enums.PaymentEventTrigger;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentCommandControllerTest extends PaymentFlowSupport {

    private static final String PAYMENTS_URL = "/api/v1/payment-engine/payments";

    @Test
    void shouldInitiateAnOutboundPaymentAndAskTheLedgerToAuthoriseIt() throws Exception {
        UUID paymentId = UUID.randomUUID();
        InitiatePaymentRequest initiatePaymentRequest = new InitiatePaymentRequest(
                paymentId, CUSTOMER_ID, SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID,
                AMOUNT, FROM_CURRENCY, null, PAYMENT_TYPE, false, DESCRIPTION);

        mockMvc.perform(post(PAYMENTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initiatePaymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.message").value("Payment initiated successfully"));

        LedgerPostingRequestedEvent authorisationRequest =
                awaitLedgerPostingRequested(paymentId, PostingInstructionType.OUTBOUND_AUTHORISATION);
        assertThat(authorisationRequest.getCustomerAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(authorisationRequest.getCurrency()).isEqualTo(FROM_CURRENCY);
        PaymentSnapshotEvent initiatedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_INITIATED);
        assertThat(initiatedSnapshot.getConvertedAmount()).isEqualTo(initiatedSnapshot.getAmount());
        assertThat(initiatedSnapshot.getToCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(initiatedSnapshot.getPaymentScheme()).isEqualTo(com.aml.payment.PaymentScheme.EXTERNAL_OUTBOUND);
    }

    @Test
    void shouldRefuseAManualReviewDecisionForAPaymentNotUnderReview() throws Exception {
        UUID paymentId = givenAuthorisedPayment();

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/manual-review/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveManualReviewRequest(null, OFFICER, APPROVAL_NOTES))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatus").value("409 CONFLICT"))
                .andExpect(jsonPath("$.message").value("Payment is not in MANUAL_REVIEW_REQUIRED status"));

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/manual-review/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectManualReviewRequest(null, OFFICER, REJECTION_REASON))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Payment is not in MANUAL_REVIEW_REQUIRED status"));
    }

    @Test
    void shouldReportAnUnknownPaymentAsNotFound() throws Exception {
        UUID unknownPaymentId = UUID.randomUUID();

        mockMvc.perform(post(PAYMENTS_URL + "/" + unknownPaymentId + "/decision/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
