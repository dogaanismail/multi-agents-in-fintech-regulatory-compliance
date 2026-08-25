package org.banksolution.fixtures;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.payment.PaymentCreatedEvent;
import com.aml.risk.CustomerAgentObservation;
import com.aml.risk.FeatureContribution;
import com.aml.risk.MarlAction;
import com.aml.risk.MarlAssessment;
import com.aml.risk.NetworkAgentObservation;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentCompletedEvent;
import com.aml.risk.RiskAssessmentRequestedEvent;
import com.aml.risk.RiskLevel;
import com.aml.risk.TransactionAgentObservation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AvroEventFixtures {

    public static final long TIMESTAMP = 1_700_000_000_000L;

    private AvroEventFixtures() {
    }

    public static PaymentCreatedEvent createPaymentCreatedEvent() {
        return createPaymentCreatedEvent(PaymentFixtures.PAYMENT_UUID);
    }

    public static PaymentCreatedEvent createPaymentCreatedEvent(UUID paymentId) {
        return PaymentCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(paymentId.toString())
                .setTimestamp(TIMESTAMP)
                .setCustomerId(PaymentFixtures.CUSTOMER_ID.toString())
                .setPaymentType(com.aml.payment.PaymentType.TRANSFER_OUT)
                .setPaymentScheme(com.aml.payment.PaymentScheme.INTERNAL_TRANSFER)
                .setFixedSide(com.aml.payment.FixedSide.SELL)
                .setIsCrossBorderPayment(false)
                .setSourceAccountId(PaymentFixtures.SOURCE_ACCOUNT_ID.toString())
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setConvertedAmount(PaymentFixtures.CONVERTED_AMOUNT.toPlainString())
                .setAppliedExchangeRate(PaymentFixtures.EXCHANGE_RATE.toPlainString())
                .setDescription(PaymentFixtures.DESCRIPTION)
                .build();
    }

    public static PaymentCreatedEvent createDepositPaymentCreatedEvent() {
        return PaymentCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setTimestamp(TIMESTAMP)
                .setCustomerId(PaymentFixtures.CUSTOMER_ID.toString())
                .setPaymentType(com.aml.payment.PaymentType.DEPOSIT)
                .setPaymentScheme(com.aml.payment.PaymentScheme.EXTERNAL_INBOUND)
                .setFixedSide(com.aml.payment.FixedSide.SELL)
                .setIsCrossBorderPayment(false)
                .setSourceAccountId(null)
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setConvertedAmount(PaymentFixtures.CONVERTED_AMOUNT.toPlainString())
                .setAppliedExchangeRate(null)
                .setDescription(null)
                .build();
    }

    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEvent(RiskAction action, RiskLevel level, double score) {
        return createRiskAssessmentCompletedEvent(PaymentFixtures.PAYMENT_UUID, action, level, score);
    }

    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEvent(
            UUID paymentId,
            RiskAction action,
            RiskLevel level,
            double score) {

        return RiskAssessmentCompletedEvent.newBuilder()
                .setRiskCheckRequestId(UUID.randomUUID().toString())
                .setPaymentId(paymentId.toString())
                .setRiskScore(score)
                .setRiskLevel(level)
                .setAction(action)
                .setFraudIndicators(List.of("NONE"))
                .setMlModelVersion("model-v1")
                .setProcessingTimeMs(12L)
                .setTimestamp(TIMESTAMP)
                .build();
    }

    public static RiskAssessmentCompletedEvent createRiskAssessmentCompletedEventWithMarl(RiskAction action) {
        return RiskAssessmentCompletedEvent.newBuilder(createRiskAssessmentCompletedEvent(action, RiskLevel.HIGH, 0.95))
                .setMarlAssessment(createMarlAssessment())
                .build();
    }

    public static MarlAssessment createMarlAssessment() {
        return MarlAssessment.newBuilder()
                .setRequestId("marl-req-1")
                .setAction(MarlAction.BLOCK)
                .setConfidence(0.91)
                .setMaddpgQValue(0.42)
                .setTransactionAgentObservation(TransactionAgentObservation.newBuilder()
                        .setAgentName("transaction-pattern-agent")
                        .setIsSuspicious(true)
                        .setProbability(0.88)
                        .setRiskScore(0.77)
                        .setConfidence("HIGH")
                        .setResponseTimeMs(12.5)
                        .setFeatureContributions(List.of(createFeatureContribution()))
                        .setShapBaseValue(0.05)
                        .build())
                .setCustomerAgentObservation(CustomerAgentObservation.newBuilder()
                        .setAgentName("customer-risk-agent")
                        .setIsSuspicious(false)
                        .setProbability(0.20)
                        .setRiskScore(0.15)
                        .setConfidence("LOW")
                        .setResponseTimeMs(8.0)
                        .setFeatureContributions(null)
                        .setShapBaseValue(null)
                        .build())
                .setNetworkAgentObservation(NetworkAgentObservation.newBuilder()
                        .setAgentName("network-analysis-agent")
                        .setIsSuspicious(true)
                        .setProbability(0.70)
                        .setRiskScore(0.65)
                        .setConfidence("MEDIUM")
                        .setResponseTimeMs(20.0)
                        .setFeatureContributions(List.of())
                        .setShapBaseValue(0.1)
                        .build())
                .setAgentContributions(Map.of("transaction", 0.5, "customer", 0.3, "network", 0.2))
                .setProcessingTimeMs(34.0)
                .setMode("inference")
                .build();
    }

    public static FeatureContribution createFeatureContribution() {
        return FeatureContribution.newBuilder()
                .setFeature("amount")
                .setValue("100.00")
                .setShapValue(0.31)
                .setDirection("increase")
                .build();
    }

    public static RiskAssessmentRequestedEvent createRiskAssessmentRequestedEvent() {
        return RiskAssessmentRequestedEvent.newBuilder()
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setCustomerId(PaymentFixtures.CUSTOMER_ID.toString())
                .setPaymentType(com.aml.risk.PaymentType.TRANSFER_OUT)
                .setTimestamp(TIMESTAMP)
                .setSourceAccountId(PaymentFixtures.SOURCE_ACCOUNT_ID.toString())
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setDescription(PaymentFixtures.DESCRIPTION)
                .build();
    }

    public static LedgerPostingCompletedEvent createLedgerPostingCompletedEvent(
            UUID paymentId,
            PostingInstructionType postingInstructionType,
            boolean success,
            UUID transferId,
            String failureReason) {

        return LedgerPostingCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(paymentId.toString())
                .setPostingInstructionType(postingInstructionType)
                .setSuccess(success)
                .setTransferId(transferId != null ? transferId.toString() : null)
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setCurrency(PaymentFixtures.FROM_CURRENCY)
                .setFailureReason(failureReason)
                .setTimestamp(TIMESTAMP)
                .build();
    }
}
