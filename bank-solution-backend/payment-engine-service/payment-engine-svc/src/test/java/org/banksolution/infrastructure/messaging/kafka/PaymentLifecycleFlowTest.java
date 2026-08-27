package org.banksolution.infrastructure.messaging.kafka;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import com.aml.feedback.FeedbackType;
import com.aml.feedback.OfficerDecision;
import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.payment.FraudCheckStatus;
import com.aml.payment.PaymentCompletedEvent;
import com.aml.payment.PaymentSnapshotEvent;
import com.aml.payment.PaymentStatus;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.api.dto.ApproveManualReviewRequest;
import org.banksolution.api.dto.OverrideDecisionRequest;
import org.banksolution.api.dto.RejectManualReviewRequest;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.enums.PaymentEventTrigger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentLifecycleFlowTest extends PaymentFlowSupport {

    private static final String PAYMENTS_URL = "/api/v1/payment-engine/payments";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAuthoriseAssessAndSettleAPaymentTheRiskEngineClearsAndReportItCompleted() throws Exception {
        UUID paymentId = givenPaymentCreated();

        LedgerPostingRequestedEvent authorisationRequest =
                awaitLedgerPostingRequested(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);
        assertThat(authorisationRequest.getAmount()).isEqualTo("100.00");
        assertThat(authorisationRequest.getCustomerAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(authorisationRequest.getCounterpartyCustomerAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());

        whenLedgerAnswers(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION, true, null);
        RiskAssessmentRequestedEvent riskAssessmentRequest = awaitRiskAssessmentRequested(paymentId);
        assertThat(riskAssessmentRequest.getCustomerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(riskAssessmentRequest.getAmount()).isEqualTo("100.00");

        whenRiskEngineDecides(paymentId, RiskAction.PROCEED);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.SETTLEMENT);

        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, true, null);
        PaymentCompletedEvent paymentCompletedEvent = awaitPaymentCompleted(paymentId);
        assertThat(paymentCompletedEvent.getRiskCheckPassed()).isTrue();
        assertThat(paymentCompletedEvent.getRiskScore()).isEqualTo(0.5);
        assertThat(paymentCompletedEvent.getProcessingTimeMs()).isGreaterThan(0);

        PaymentSnapshotEvent initiatedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_INITIATED);
        PaymentSnapshotEvent completedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        assertThat(completedSnapshot.getVersion()).isGreaterThan(initiatedSnapshot.getVersion());
        assertThat(completedSnapshot.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completedSnapshot.getFraudStatus()).isEqualTo(FraudCheckStatus.APPROVED);
        assertThat(completedSnapshot.getRiskAssessment().getRiskAction()).isEqualTo("PROCEED");
        assertThat(completedSnapshot.getInitiatedAt()).isNotNull();
        assertThat(completedSnapshot.getLedgerAuthorisedAt()).isGreaterThanOrEqualTo(completedSnapshot.getInitiatedAt());
        assertThat(completedSnapshot.getLedgerSettledAt()).isGreaterThanOrEqualTo(completedSnapshot.getLedgerAuthorisedAt());
        assertThat(completedSnapshot.getCompletedAt()).isGreaterThanOrEqualTo(completedSnapshot.getLedgerSettledAt());
        assertThat(completedSnapshot.getConvertedAmount()).isEqualTo("100.00");
    }

    @Test
    void shouldReleaseTheHeldFundsAndReportBlockedWhenTheRiskEngineBlocks() throws Exception {
        UUID paymentId = givenAuthorisedPayment();

        whenRiskEngineDecides(paymentId, RiskAction.BLOCK);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.RELEASE);
        PaymentSnapshotEvent blockedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_BLOCKED);
        assertThat(blockedSnapshot.getBlockReason()).isEqualTo("Risk level: HIGH, Risk score: 0.5");

        whenLedgerAnswers(paymentId, PostingInstructionType.RELEASE, true, null);
        PaymentCompletedEvent paymentCompletedEvent = awaitPaymentCompleted(paymentId);
        assertThat(paymentCompletedEvent.getRiskCheckPassed()).isFalse();
        PaymentSnapshotEvent completedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        assertThat(completedSnapshot.getStatus()).isEqualTo(PaymentStatus.BLOCKED);
        assertThat(completedSnapshot.getFraudStatus()).isEqualTo(FraudCheckStatus.BLOCKED);
        assertThat(completedSnapshot.getBlockedAt()).isNotNull();
    }

    @Test
    void shouldFailThePaymentWithoutAssessingRiskWhenTheLedgerDeclinesAuthorisation() throws Exception {
        UUID paymentId = givenPaymentCreated();
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);

        whenLedgerAnswers(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION, false, "Insufficient funds");

        PaymentSnapshotEvent completedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        assertThat(completedSnapshot.getStatus()).isEqualTo(PaymentStatus.AUTHORISATION_DECLINED);
        assertThat(completedSnapshot.getFailureReason()).isEqualTo("Insufficient funds");
        assertThat(completedSnapshot.getRiskCheckRequestedAt()).isNull();
        assertThat(awaitPaymentCompleted(paymentId).getRiskCheckPassed()).isFalse();
    }

    @Test
    void shouldFailThePaymentWhenTheLedgerCannotSettle() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.PROCEED);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.SETTLEMENT);

        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, false, "Pending transfer expired");

        PaymentSnapshotEvent completedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        assertThat(completedSnapshot.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(completedSnapshot.getFailureReason()).isEqualTo("Pending transfer expired");
    }

    @Test
    void shouldHoldForManualReviewSettleOnApprovalAndFeedTheOfficerDecisionBackToTheAgents() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.ESCALATE);
        PaymentSnapshotEvent reviewSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.MANUAL_REVIEW_REQUESTED);
        assertThat(reviewSnapshot.getStatus()).isEqualTo(PaymentStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(reviewSnapshot.getFraudStatus()).isEqualTo(FraudCheckStatus.REVIEW_REQUIRED);

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/manual-review/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveManualReviewRequest(null, OFFICER, APPROVAL_NOTES))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.reviewedBy").value(OFFICER));

        awaitLedgerPostingRequested(paymentId, PostingInstructionType.SETTLEMENT);
        ComplianceAgentManualFeedbackEvent complianceAgentManualFeedbackEvent = awaitComplianceFeedback(paymentId);
        assertThat(complianceAgentManualFeedbackEvent.getFeedbackType()).isEqualTo(FeedbackType.MANUAL_REVIEW);
        assertThat(complianceAgentManualFeedbackEvent.getOfficerDecision()).isEqualTo(OfficerDecision.APPROVE);
        assertThat(complianceAgentManualFeedbackEvent.getReviewedBy()).isEqualTo(OFFICER);
        assertThat(complianceAgentManualFeedbackEvent.getOriginalMarlAction()).isEqualTo("UNKNOWN");

        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, true, null);
        PaymentSnapshotEvent completedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        assertThat(completedSnapshot.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(completedSnapshot.getManualReviewedBy()).isEqualTo(OFFICER);
        assertThat(completedSnapshot.getManualReviewNotes()).isEqualTo(APPROVAL_NOTES);
        assertThat(completedSnapshot.getManualReviewApprovedAt()).isNotNull();
    }

    @Test
    void shouldReleaseTheFundsWhenTheOfficerRejectsTheReview() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.ESCALATE);
        awaitSnapshot(paymentId, PaymentEventTrigger.MANUAL_REVIEW_REQUESTED);

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/manual-review/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectManualReviewRequest(null, OFFICER, REJECTION_REASON))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manual review rejected. Payment has been blocked: " + REJECTION_REASON));

        awaitLedgerPostingRequested(paymentId, PostingInstructionType.RELEASE);
        assertThat(awaitComplianceFeedback(paymentId).getOfficerDecision()).isEqualTo(OfficerDecision.REJECT);

        whenLedgerAnswers(paymentId, PostingInstructionType.RELEASE, true, null);
        PaymentSnapshotEvent completedSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        assertThat(completedSnapshot.getStatus()).isEqualTo(PaymentStatus.BLOCKED);
        assertThat(completedSnapshot.getBlockReason()).isEqualTo("Manual review rejected: " + REJECTION_REASON);
    }

    @Test
    void shouldLetAnOfficerOverrideABlockAndReportTheOverrideToTheAgents() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.BLOCK);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.RELEASE);
        whenLedgerAnswers(paymentId, PostingInstructionType.RELEASE, true, null);
        awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/decision/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("OVERRIDE_APPROVED"));

        PaymentSnapshotEvent overrideSnapshot = awaitSnapshot(paymentId, PaymentEventTrigger.DECISION_OVERRIDE_APPROVED);
        assertThat(overrideSnapshot.getStatus()).isEqualTo(PaymentStatus.OVERRIDE_APPROVED);
        assertThat(overrideSnapshot.getDecisionOverriddenBy()).isEqualTo(OFFICER);
        assertThat(overrideSnapshot.getDecisionOverrideReason()).isEqualTo(OVERRIDE_REASON);
        assertThat(overrideSnapshot.getDecisionOverriddenAt()).isNotNull();
        ComplianceAgentManualFeedbackEvent complianceAgentManualFeedbackEvent = awaitComplianceFeedback(paymentId);
        assertThat(complianceAgentManualFeedbackEvent.getFeedbackType()).isEqualTo(FeedbackType.DECISION_OVERRIDE);
        assertThat(complianceAgentManualFeedbackEvent.getOriginalMarlAction()).isEqualTo("BLOCK");
        assertThat(complianceAgentManualFeedbackEvent.getOfficerDecision()).isEqualTo(OfficerDecision.APPROVE);
    }

    @Test
    void shouldIgnoreARedeliveredLedgerOutcomeOnceThePaymentHasMovedOn() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.PROCEED);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.SETTLEMENT);
        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, true, null);
        awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);

        whenLedgerAnswers(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION, true, null);
        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, true, null);

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/decision/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Decision override is only allowed for BLOCKED payments, current status: COMPLETED"));
    }

    /**
     * Reject, release and override make a 10-event stream, which is where Axon's snapshotter
     * kicks in; the second override must then be judged on state loaded from that snapshot.
     */
    @Test
    void shouldReloadTheAggregateFromItsSnapshotWithTheSameState() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.ESCALATE);
        awaitSnapshot(paymentId, PaymentEventTrigger.MANUAL_REVIEW_REQUESTED);
        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/manual-review/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectManualReviewRequest(null, OFFICER, REJECTION_REASON))))
                .andExpect(status().isOk());
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.RELEASE);
        whenLedgerAnswers(paymentId, PostingInstructionType.RELEASE, true, null);
        awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);
        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/decision/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, false))))
                .andExpect(status().isOk());
        awaitSnapshot(paymentId, PaymentEventTrigger.DECISION_OVERRIDE_REJECTED);

        await().atMost(FLOW_TIMEOUT).untilAsserted(() -> assertThat(countAxonSnapshots(paymentId)).isPositive());

        mockMvc.perform(post(PAYMENTS_URL + "/" + paymentId + "/decision/override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OverrideDecisionRequest(null, OFFICER, OVERRIDE_REASON, true))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Decision override is only allowed for BLOCKED payments, current status: OVERRIDE_REJECTED"));
    }

    private int countAxonSnapshots(UUID paymentId) {
        Integer snapshotCount = jdbcTemplate.queryForObject(
                "select count(*) from snapshot_event_entry where aggregate_identifier = ?",
                Integer.class,
                paymentId.toString());
        return snapshotCount != null ? snapshotCount : 0;
    }
}
