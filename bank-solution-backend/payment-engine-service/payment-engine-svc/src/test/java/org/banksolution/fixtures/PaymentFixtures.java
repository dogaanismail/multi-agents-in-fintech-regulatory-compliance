package org.banksolution.fixtures;

import org.banksolution.domain.payment.command.*;
import org.banksolution.domain.payment.event.*;
import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.ApproveManualReviewCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.command.OverrideDecisionCommand;
import org.banksolution.domain.payment.command.RejectManualReviewCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.event.FraudCheckApprovedEvent;
import org.banksolution.domain.payment.event.ManualReviewRequestedEvent;
import org.banksolution.domain.payment.event.PaymentBlockedEvent;
import org.banksolution.domain.payment.event.PaymentCompletedEvent;
import org.banksolution.domain.payment.event.PaymentInitiatedEvent;
import org.banksolution.domain.payment.event.RiskAssessmentCompletedEvent;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
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

    private PaymentFixtures() {
    }

    public static PaymentId createPaymentId() {
        return new PaymentId(PAYMENT_UUID);
    }

    public static RiskAssessment createRiskAssessment(String action, String level, double score) {
        return new RiskAssessment("risk-req-1", score, level, action, List.of("NONE"), "model-v1", 12L, null);
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
        return new ApproveManualReviewCommand(createPaymentId(), "officer-1", "Looks legitimate");
    }

    public static RejectManualReviewCommand createRejectManualReviewCommand() {
        return new RejectManualReviewCommand(createPaymentId(), "officer-1", "Confirmed fraud");
    }

    public static OverrideDecisionCommand createOverrideDecisionCommand() {
        return new OverrideDecisionCommand(createPaymentId(), "officer-1", "False positive", true);
    }


    public static OverrideDecisionCommand createOverrideDecisionCommand(boolean approvePayment) {
        return new OverrideDecisionCommand(createPaymentId(), "officer-1", "False positive", approvePayment);
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
        return new ManualReviewRequestedEvent(createPaymentId(), riskAssessment.riskScore(), null, riskAssessment);
    }

    public static PaymentBlockedEvent createPaymentBlockedEvent(RiskAssessment riskAssessment) {
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());
        return new PaymentBlockedEvent(createPaymentId(), reason, riskAssessment.riskScore(), null, riskAssessment);
    }


    public static PaymentCompletedEvent createPaymentCompletedEvent(PaymentStatus finalStatus, String reason) {
        return new PaymentCompletedEvent(createPaymentId(), finalStatus, reason);
    }


    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEventWithoutAssessment() {
        return new RiskAssessmentCompletedEvent(createPaymentId(), null);
    }

    public static LedgerAuthorisationInitiatedEvent createLedgerAuthorisationInitiatedEvent() {
        return new LedgerAuthorisationInitiatedEvent(
                createPaymentId(),
                CUSTOMER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                CONVERTED_AMOUNT,
                TO_CURRENCY,
                PAYMENT_TYPE,
                PAYMENT_SCHEME,
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
        return new ManualReviewApprovedEvent(createPaymentId(), "officer-1", "Looks legitimate");
    }

    public static ManualReviewRejectedEvent createManualReviewRejectedEvent() {
        return new ManualReviewRejectedEvent(createPaymentId(), "officer-1", "Confirmed fraud");
    }
}
