package org.banksolution.fixtures;

import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.ApproveManualReviewCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.ChargeAccountCommand;
import org.banksolution.domain.payment.command.ConfirmAccountChargedCommand;
import org.banksolution.domain.payment.command.FailAccountChargeCommand;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.command.OverrideDecisionCommand;
import org.banksolution.domain.payment.command.RejectManualReviewCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.event.AccountChargeFailedEvent;
import org.banksolution.domain.payment.event.AccountChargeInitiatedEvent;
import org.banksolution.domain.payment.event.AccountChargedEvent;
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
    public static final String DESCRIPTION = "Test payment";

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

    public static ChargeAccountCommand createChargeAccountCommand() {
        return new ChargeAccountCommand(
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
                DESCRIPTION
        );
    }

    public static OverrideDecisionCommand createOverrideDecisionCommand(boolean approvePayment) {
        return new OverrideDecisionCommand(createPaymentId(), "officer-1", "False positive", approvePayment);
    }

    public static ConfirmAccountChargedCommand createConfirmAccountChargedCommand() {
        return new ConfirmAccountChargedCommand(
                createPaymentId(),
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                PAYMENT_TYPE
        );
    }

    public static FailAccountChargeCommand createFailAccountChargeCommand(String reason) {
        return new FailAccountChargeCommand(createPaymentId(), reason);
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

    public static AccountChargeInitiatedEvent createAccountChargeInitiatedEvent() {
        return new AccountChargeInitiatedEvent(
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
                DESCRIPTION
        );
    }

    public static AccountChargedEvent createAccountChargedEvent() {
        return new AccountChargedEvent(
                createPaymentId(),
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                PAYMENT_TYPE
        );
    }

    public static PaymentCompletedEvent createPaymentCompletedEvent(PaymentStatus finalStatus, String reason) {
        return new PaymentCompletedEvent(createPaymentId(), finalStatus, reason);
    }

    public static AccountChargeFailedEvent createAccountChargeFailedEvent(String reason) {
        return new AccountChargeFailedEvent(createPaymentId(), reason);
    }

    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEventWithoutAssessment() {
        return new RiskAssessmentCompletedEvent(createPaymentId(), null);
    }
}
