package org.banksolution.fixtures;

import org.banksolution.domain.payment.command.*;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.FeatureContribution;
import org.banksolution.domain.payment.valueobject.MarlAssessment;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PaymentFixtures {

    public static final UUID PAYMENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID SOURCE_ACCOUNT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DESTINATION_ACCOUNT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final BigDecimal AMOUNT = new BigDecimal("100.00");
    public static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("100.00");
    public static final BigDecimal EXCHANGE_RATE = new BigDecimal("1.00");
    public static final String FROM_CURRENCY = "GBP";
    public static final String TO_CURRENCY = "GBP";
    public static final String PAYMENT_TYPE = "TRANSFER_OUT";
    public static final String PAYMENT_SCHEME = "INTERNAL_TRANSFER";
    public static final String FIXED_SIDE = "SELL";
    public static final String DESCRIPTION = "Test payment";
    public static final UUID AUTHORISATION_TRANSFER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID SETTLEMENT_TRANSFER_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final String OFFICER = "officer-1";
    public static final String APPROVAL_NOTES = "Looks legitimate";
    public static final String REJECTION_REASON = "Confirmed fraud";
    public static final String OVERRIDE_REASON = "False positive";
    public static final Instant INITIATED_AT = Instant.parse("2026-08-26T10:00:00Z");
    public static final Instant COMPLETED_AT = Instant.parse("2026-08-26T10:00:05Z");

    private PaymentFixtures() {
    }

    public static PaymentId createPaymentId() {
        return new PaymentId(PAYMENT_UUID);
    }

    public static RiskAssessment createRiskAssessment(String action, String level, double score) {
        return new RiskAssessment("risk-req-1", score, level, action, List.of("NONE"), "model-v1", 12L, null);
    }

    public static RiskAssessment createRiskAssessmentWithMarl(String action, String level, double score) {
        return new RiskAssessment("risk-req-1", score, level, action, List.of("VELOCITY"), "model-v1", 12L,
                createMarlAssessment());
    }

    public static MarlAssessment createMarlAssessment() {
        return new MarlAssessment(
                "marl-req-1",
                "BLOCK",
                0.91,
                0.42,
                createAgentObservation("transaction-pattern-agent"),
                createAgentObservation("customer-risk-agent"),
                createAgentObservation("network-analysis-agent"),
                Map.of("transaction", 0.5, "customer", 0.3, "network", 0.2),
                34L,
                "inference");
    }

    public static AgentObservation createAgentObservation(String agentName) {
        return new AgentObservation(
                agentName,
                true,
                0.88,
                0.77,
                "HIGH",
                12.5,
                List.of(new FeatureContribution("amount", "100.00", 0.31, "increase")),
                0.05);
    }

    public static RiskAssessment createProceedAssessment() {
        return createRiskAssessment("PROCEED", "LOW", 0.10);
    }

    public static RiskAssessment createEscalateAssessment() {
        return createRiskAssessment("ESCALATE", "MEDIUM", 0.60);
    }

    public static RiskAssessment createBlockAssessment() {
        return createRiskAssessment("BLOCK", "HIGH", 0.95);
    }

    public static InitiatePaymentCommand createInitiatePaymentCommand() {
        return new InitiatePaymentCommand(
                createPaymentId(),
                CUSTOMER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                CONVERTED_AMOUNT,
                EXCHANGE_RATE,
                PAYMENT_TYPE,
                PAYMENT_SCHEME,
                FIXED_SIDE,
                false,
                DESCRIPTION
        );
    }

    public static ApproveFraudCheckCommand createApproveFraudCheckCommand(RiskAssessment riskAssessment) {
        return new ApproveFraudCheckCommand(createPaymentId(), riskAssessment);
    }

    public static RequestManualReviewCommand createRequestManualReviewCommand(RiskAssessment riskAssessment) {
        return new RequestManualReviewCommand(createPaymentId(), riskAssessment);
    }

    public static BlockPaymentCommand createBlockPaymentCommand(RiskAssessment riskAssessment) {
        return new BlockPaymentCommand(createPaymentId(), riskAssessment);
    }

    public static ApproveManualReviewCommand createApproveManualReviewCommand() {
        return new ApproveManualReviewCommand(createPaymentId(), OFFICER, APPROVAL_NOTES);
    }

    public static RejectManualReviewCommand createRejectManualReviewCommand() {
        return new RejectManualReviewCommand(createPaymentId(), OFFICER, REJECTION_REASON);
    }

    public static OverrideDecisionCommand createOverrideDecisionCommand(boolean approvePayment) {
        return new OverrideDecisionCommand(createPaymentId(), OFFICER, OVERRIDE_REASON, approvePayment);
    }

    public static PaymentInitiatedEvent createPaymentInitiatedEvent() {
        return new PaymentInitiatedEvent(
                createPaymentId(),
                CUSTOMER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                CONVERTED_AMOUNT,
                EXCHANGE_RATE,
                PAYMENT_TYPE,
                PAYMENT_SCHEME,
                FIXED_SIDE,
                false,
                DESCRIPTION
        );
    }

    public static RiskAssessmentInitiatedEvent createRiskAssessmentInitiatedEvent() {
        return new RiskAssessmentInitiatedEvent(
                createPaymentId(),
                CUSTOMER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                PAYMENT_TYPE,
                DESCRIPTION
        );
    }

    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEvent(RiskAssessment riskAssessment) {
        return new RiskAssessmentCompletedEvent(createPaymentId(), riskAssessment);
    }

    public static FraudCheckApprovedEvent createFraudCheckApprovedEvent(RiskAssessment riskAssessment) {
        return new FraudCheckApprovedEvent(createPaymentId(), riskAssessment);
    }

    public static ManualReviewRequestedEvent createManualReviewRequestedEvent(RiskAssessment riskAssessment) {
        return new ManualReviewRequestedEvent(createPaymentId(), riskAssessment.riskScore(), maddpgQValue(riskAssessment), riskAssessment);
    }

    public static PaymentBlockedEvent createPaymentBlockedEvent(RiskAssessment riskAssessment) {
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());
        return new PaymentBlockedEvent(createPaymentId(), reason, riskAssessment.riskScore(), maddpgQValue(riskAssessment), riskAssessment);
    }

    private static Double maddpgQValue(RiskAssessment riskAssessment) {
        return riskAssessment.marlAssessment() != null ? riskAssessment.marlAssessment().maddpgQValue() : null;
    }

    public static PaymentCompletedEvent createPaymentCompletedEvent(PaymentStatus finalStatus, String reason) {
        return new PaymentCompletedEvent(createPaymentId(), finalStatus, reason);
    }

    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEventWithoutAssessment() {
        return new RiskAssessmentCompletedEvent(createPaymentId(), null);
    }

    public static LedgerAuthorisationInitiatedEvent createLedgerAuthorisationInitiatedEvent() {
        return createLedgerAuthorisationInitiatedEvent(PAYMENT_SCHEME, TO_CURRENCY);
    }

    public static LedgerAuthorisationInitiatedEvent createLedgerAuthorisationInitiatedEvent(
            String paymentScheme,
            String toCurrency) {

        return new LedgerAuthorisationInitiatedEvent(
                createPaymentId(),
                CUSTOMER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                CONVERTED_AMOUNT,
                toCurrency,
                PAYMENT_TYPE,
                paymentScheme,
                DESCRIPTION
        );
    }

    public static ConfirmLedgerAuthorisationCommand createConfirmLedgerAuthorisationCommand() {
        return new ConfirmLedgerAuthorisationCommand(createPaymentId(), AUTHORISATION_TRANSFER_ID);
    }

    public static DeclineLedgerAuthorisationCommand createDeclineLedgerAuthorisationCommand(String reason) {
        return new DeclineLedgerAuthorisationCommand(createPaymentId(), reason);
    }

    public static LedgerAuthorisedEvent createLedgerAuthorisedEvent() {
        return new LedgerAuthorisedEvent(createPaymentId(), AUTHORISATION_TRANSFER_ID);
    }

    public static ConfirmLedgerSettlementCommand createConfirmLedgerSettlementCommand() {
        return new ConfirmLedgerSettlementCommand(createPaymentId(), SETTLEMENT_TRANSFER_ID);
    }

    public static FailLedgerSettlementCommand createFailLedgerSettlementCommand(String reason) {
        return new FailLedgerSettlementCommand(createPaymentId(), reason);
    }

    public static ConfirmLedgerReleaseCommand createConfirmLedgerReleaseCommand() {
        return new ConfirmLedgerReleaseCommand(createPaymentId());
    }

    public static FailLedgerReleaseCommand createFailLedgerReleaseCommand(String reason) {
        return new FailLedgerReleaseCommand(createPaymentId(), reason);
    }

    public static ExpireRiskAssessmentCommand createExpireRiskAssessmentCommand() {
        return new ExpireRiskAssessmentCommand(createPaymentId());
    }

    public static RiskAssessmentTimedOutEvent createRiskAssessmentTimedOutEvent() {
        return new RiskAssessmentTimedOutEvent(createPaymentId());
    }

    public static LedgerReleaseFailedEvent createLedgerReleaseFailedEvent(String reason) {
        return new LedgerReleaseFailedEvent(createPaymentId(), reason);
    }

    public static LedgerSettlementInitiatedEvent createLedgerSettlementInitiatedEvent() {
        return new LedgerSettlementInitiatedEvent(createPaymentId());
    }

    public static LedgerReleaseInitiatedEvent createLedgerReleaseInitiatedEvent() {
        return new LedgerReleaseInitiatedEvent(createPaymentId());
    }

    public static LedgerSettledEvent createLedgerSettledEvent() {
        return new LedgerSettledEvent(createPaymentId(), SETTLEMENT_TRANSFER_ID);
    }

    public static LedgerSettlementFailedEvent createLedgerSettlementFailedEvent(String reason) {
        return new LedgerSettlementFailedEvent(createPaymentId(), reason);
    }

    public static LedgerAuthorisationDeclinedEvent createLedgerAuthorisationDeclinedEvent(String reason) {
        return new LedgerAuthorisationDeclinedEvent(createPaymentId(), reason);
    }

    public static LedgerReleasedEvent createLedgerReleasedEvent() {
        return new LedgerReleasedEvent(createPaymentId());
    }

    public static ManualReviewApprovedEvent createManualReviewApprovedEvent() {
        return new ManualReviewApprovedEvent(createPaymentId(), OFFICER, APPROVAL_NOTES);
    }

    public static ManualReviewRejectedEvent createManualReviewRejectedEvent() {
        return new ManualReviewRejectedEvent(createPaymentId(), OFFICER, REJECTION_REASON);
    }

    public static DecisionOverriddenEvent createDecisionOverriddenEvent(boolean approvePayment, String originalStatus) {
        return new DecisionOverriddenEvent(createPaymentId(), OFFICER, OVERRIDE_REASON, approvePayment, originalStatus);
    }

    public static PaymentResponse createPaymentResponse(
            PaymentStatus paymentStatus,
            FraudAnalysisStatus fraudAnalysisStatus,
            RiskAssessment riskAssessment) {

        return new PaymentResponse(
                PAYMENT_UUID.toString(),
                "PAY-11111111",
                CUSTOMER_ID.toString(),
                SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString(),
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                CONVERTED_AMOUNT,
                EXCHANGE_RATE,
                PaymentType.TRANSFER_OUT,
                PAYMENT_SCHEME,
                DESCRIPTION,
                false,
                paymentStatus,
                fraudAnalysisStatus,
                riskAssessment,
                7L,
                INITIATED_AT,
                INITIATED_AT.plusSeconds(1),
                INITIATED_AT.plusSeconds(2),
                INITIATED_AT.plusSeconds(2),
                null,
                null,
                null,
                INITIATED_AT,
                INITIATED_AT.plusSeconds(1),
                INITIATED_AT.plusSeconds(3),
                INITIATED_AT.plusSeconds(4),
                null,
                null,
                COMPLETED_AT,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
