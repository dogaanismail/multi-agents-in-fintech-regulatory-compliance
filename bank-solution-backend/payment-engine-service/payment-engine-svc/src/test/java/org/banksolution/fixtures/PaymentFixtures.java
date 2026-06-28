package org.banksolution.fixtures;

import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.ApproveManualReviewCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.ConfirmAccountChargedCommand;
import org.banksolution.domain.payment.command.FailAccountChargeCommand;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.domain.payment.command.OverrideDecisionCommand;
import org.banksolution.domain.payment.command.RejectManualReviewCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
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

    public static PaymentId paymentId() {
        return new PaymentId(PAYMENT_UUID);
    }

    public static RiskAssessment riskAssessment(String action, String level, double score) {
        return new RiskAssessment("risk-req-1", score, level, action, List.of("NONE"), "model-v1", 12L, null);
    }

    public static RiskAssessment proceedAssessment() {
        return riskAssessment("PROCEED", "LOW", 0.10);
    }

    public static RiskAssessment escalateAssessment() {
        return riskAssessment("ESCALATE", "MEDIUM", 0.60);
    }

    public static RiskAssessment blockAssessment() {
        return riskAssessment("BLOCK", "HIGH", 0.95);
    }

    public static InitiatePaymentCommand initiatePaymentCommand() {
        return new InitiatePaymentCommand(
                paymentId(),
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

    public static ApproveFraudCheckCommand approveFraudCheckCommand(RiskAssessment riskAssessment) {
        return new ApproveFraudCheckCommand(paymentId(), riskAssessment);
    }

    public static RequestManualReviewCommand requestManualReviewCommand(RiskAssessment riskAssessment) {
        return new RequestManualReviewCommand(paymentId(), riskAssessment);
    }

    public static BlockPaymentCommand blockPaymentCommand(RiskAssessment riskAssessment) {
        return new BlockPaymentCommand(paymentId(), riskAssessment);
    }

    public static ApproveManualReviewCommand approveManualReviewCommand() {
        return new ApproveManualReviewCommand(paymentId(), "officer-1", "Looks legitimate");
    }

    public static RejectManualReviewCommand rejectManualReviewCommand() {
        return new RejectManualReviewCommand(paymentId(), "officer-1", "Confirmed fraud");
    }

    public static OverrideDecisionCommand overrideDecisionCommand() {
        return new OverrideDecisionCommand(paymentId(), "officer-1", "False positive", true);
    }

    public static ConfirmAccountChargedCommand confirmAccountChargedCommand() {
        return new ConfirmAccountChargedCommand(
                paymentId(),
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                PAYMENT_TYPE
        );
    }

    public static FailAccountChargeCommand failAccountChargeCommand(String reason) {
        return new FailAccountChargeCommand(paymentId(), reason);
    }

    public static PaymentInitiatedEvent paymentInitiatedEvent() {
        return new PaymentInitiatedEvent(
                paymentId(),
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

    public static RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent() {
        return new RiskAssessmentInitiatedEvent(
                paymentId(),
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

    public static RiskAssessmentCompletedEvent riskAssessmentCompletedEvent(RiskAssessment riskAssessment) {
        return new RiskAssessmentCompletedEvent(paymentId(), riskAssessment);
    }

    public static FraudCheckApprovedEvent fraudCheckApprovedEvent(RiskAssessment riskAssessment) {
        return new FraudCheckApprovedEvent(paymentId(), riskAssessment);
    }

    public static ManualReviewRequestedEvent manualReviewRequestedEvent(RiskAssessment riskAssessment) {
        return new ManualReviewRequestedEvent(paymentId(), riskAssessment.riskScore(), null, riskAssessment);
    }

    public static PaymentBlockedEvent paymentBlockedEvent(RiskAssessment riskAssessment) {
        String reason = String.format("Risk level: %s, Risk score: %s",
                riskAssessment.riskLevel(), riskAssessment.riskScore());
        return new PaymentBlockedEvent(paymentId(), reason, riskAssessment.riskScore(), null, riskAssessment);
    }

    public static AccountChargeInitiatedEvent accountChargeInitiatedEvent() {
        return new AccountChargeInitiatedEvent(
                paymentId(),
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

    public static AccountChargedEvent accountChargedEvent() {
        return new AccountChargedEvent(
                paymentId(),
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                AMOUNT,
                FROM_CURRENCY,
                TO_CURRENCY,
                PAYMENT_TYPE
        );
    }

    public static PaymentCompletedEvent paymentCompletedEvent(PaymentStatus finalStatus, String reason) {
        return new PaymentCompletedEvent(paymentId(), finalStatus, reason);
    }
}
