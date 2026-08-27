package org.banksolution.fixtures;

import com.aml.payment.CustomerAgentObservationSnapshot;
import com.aml.payment.FeatureContribution;
import com.aml.payment.FraudCheckStatus;
import com.aml.payment.MarlAssessmentSnapshot;
import com.aml.payment.NetworkAgentObservationSnapshot;
import com.aml.payment.PaymentScheme;
import com.aml.payment.PaymentSnapshotEvent;
import com.aml.payment.PaymentStatus;
import com.aml.payment.RiskAssessmentSnapshot;
import com.aml.payment.TransactionAgentObservationSnapshot;
import org.banksolution.entity.PaymentHistoryEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PaymentHistoryFixtures {

    public static final UUID CUSTOMER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID SOURCE_ACCOUNT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID DESTINATION_ACCOUNT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final Instant INITIATED_AT = Instant.parse("2026-08-27T10:00:00Z");
    public static final Instant COMPLETED_AT = Instant.parse("2026-08-27T10:00:05Z");
    public static final String OFFICER = "officer-1";

    private PaymentHistoryFixtures() {
    }

    public static PaymentSnapshotEvent createCompletedPaymentSnapshotEvent(UUID paymentId, UUID customerId) {
        return newPaymentSnapshotEventBuilder(paymentId, customerId)
                .setStatus(PaymentStatus.COMPLETED)
                .setFraudStatus(FraudCheckStatus.APPROVED)
                .setEventTrigger("PAYMENT_COMPLETED")
                .setRiskAssessment(createRiskAssessmentSnapshot(true))
                .setRiskCheckRequestedAt(INITIATED_AT.plusSeconds(1).toEpochMilli())
                .setRiskCheckCompletedAt(INITIATED_AT.plusSeconds(2).toEpochMilli())
                .setFraudCheckApprovedAt(INITIATED_AT.plusSeconds(2).toEpochMilli())
                .setLedgerAuthorisationInitiatedAt(INITIATED_AT.toEpochMilli())
                .setLedgerAuthorisedAt(INITIATED_AT.plusSeconds(1).toEpochMilli())
                .setLedgerSettlementInitiatedAt(INITIATED_AT.plusSeconds(3).toEpochMilli())
                .setLedgerSettledAt(INITIATED_AT.plusSeconds(4).toEpochMilli())
                .setCompletedAt(COMPLETED_AT.toEpochMilli())
                .setVersion(7)
                .build();
    }

    public static PaymentSnapshotEvent createInitiatedPaymentSnapshotEvent(UUID paymentId, UUID customerId) {
        return newPaymentSnapshotEventBuilder(paymentId, customerId)
                .setStatus(PaymentStatus.INITIATED)
                .setFraudStatus(FraudCheckStatus.PENDING)
                .setEventTrigger("PAYMENT_INITIATED")
                .setAppliedExchangeRate(null)
                .setDescription(null)
                .setVersion(0)
                .build();
    }

    public static PaymentSnapshotEvent createBlockedPaymentSnapshotEvent(UUID paymentId, UUID customerId) {
        return newPaymentSnapshotEventBuilder(paymentId, customerId)
                .setStatus(PaymentStatus.BLOCKED)
                .setFraudStatus(FraudCheckStatus.BLOCKED)
                .setEventTrigger("PAYMENT_BLOCKED")
                .setRiskAssessment(createRiskAssessmentSnapshot(true))
                .setBlockedAt(COMPLETED_AT.toEpochMilli())
                .setBlockReason("Risk level: HIGH, Risk score: 0.95")
                .setManualReviewedBy(OFFICER)
                .setManualReviewNotes("Confirmed fraud")
                .setManualReviewRejectedAt(COMPLETED_AT.toEpochMilli())
                .setDecisionOverriddenBy(OFFICER)
                .setDecisionOverrideReason("False positive")
                .setDecisionOverriddenAt(COMPLETED_AT.plusSeconds(60).toEpochMilli())
                .setFailureReason(null)
                .setVersion(9)
                .build();
    }

    public static PaymentSnapshotEvent createMalformedPaymentSnapshotEvent(String paymentId) {
        return newPaymentSnapshotEventBuilder(UUID.randomUUID(), CUSTOMER_ID)
                .setPaymentId(paymentId)
                .setReferenceNumber("PAY-" + paymentId)
                .setStatus(PaymentStatus.INITIATED)
                .setFraudStatus(FraudCheckStatus.PENDING)
                .setEventTrigger("PAYMENT_INITIATED")
                .build();
    }

    public static RiskAssessmentSnapshot createRiskAssessmentSnapshot(boolean withMarlAssessment) {
        return RiskAssessmentSnapshot.newBuilder()
                .setRiskScore(0.95)
                .setRiskLevel("HIGH")
                .setRiskAction("BLOCK")
                .setFraudIndicators(List.of("VELOCITY", "NEW_PAYEE"))
                .setMlModelVersion("model-v1")
                .setProcessingTimeMs(12L)
                .setMarlAssessment(withMarlAssessment ? createMarlAssessmentSnapshot() : null)
                .build();
    }

    public static MarlAssessmentSnapshot createMarlAssessmentSnapshot() {
        return MarlAssessmentSnapshot.newBuilder()
                .setRequestId("marl-req-1")
                .setAction("BLOCK")
                .setConfidence(0.91)
                .setMaddpgQValue(0.42)
                .setTransactionAgentObservation(TransactionAgentObservationSnapshot.newBuilder()
                        .setAgentName("transaction-pattern-agent")
                        .setIsSuspicious(true)
                        .setProbability(0.88)
                        .setRiskScore(0.77)
                        .setConfidence("HIGH")
                        .setResponseTimeMs(12.5)
                        .setFeatureContributions(List.of(FeatureContribution.newBuilder()
                                .setFeature("amount").setValue("100.00").setShapValue(0.31).setDirection("increase").build()))
                        .setShapBaseValue(0.05)
                        .build())
                .setCustomerAgentObservation(CustomerAgentObservationSnapshot.newBuilder()
                        .setAgentName("customer-risk-agent")
                        .setIsSuspicious(false)
                        .setProbability(0.20)
                        .setRiskScore(0.15)
                        .setConfidence("LOW")
                        .setResponseTimeMs(8.0)
                        .setFeatureContributions(null)
                        .setShapBaseValue(null)
                        .build())
                .setNetworkAgentObservation(NetworkAgentObservationSnapshot.newBuilder()
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

    public static PaymentHistoryEntity createPaymentHistoryEntity(UUID paymentId, UUID customerId) {
        return PaymentHistoryEntity.builder()
                .paymentId(paymentId)
                .referenceNumber("PAY-" + paymentId.toString().substring(0, 8).toUpperCase())
                .customerId(customerId)
                .sourceAccountId(SOURCE_ACCOUNT_ID)
                .destinationAccountId(DESTINATION_ACCOUNT_ID)
                .amount(new BigDecimal("100.00"))
                .fromCurrency("GBP")
                .toCurrency("EUR")
                .convertedAmount(new BigDecimal("116.0000"))
                .appliedExchangeRate(new BigDecimal("1.16000000"))
                .paymentType("TRANSFER_OUT")
                .description("Rent")
                .status("COMPLETED")
                .fraudStatus("APPROVED")
                .riskScore(0.10)
                .riskLevel("LOW")
                .riskAction("PROCEED")
                .fraudIndicators(List.of("NONE"))
                .initiatedAt(INITIATED_AT)
                .completedAt(COMPLETED_AT)
                .riskProcessingTimeMs(12L)
                .mlModelVersion("model-v1")
                .aggregateVersion(7)
                .build();
    }

    public static PaymentHistoryEntity.MarlAssessment createMarlAssessment() {
        PaymentHistoryEntity.FeatureContribution featureContribution =
                new PaymentHistoryEntity.FeatureContribution("amount", "100.00", 0.31, "increase");
        PaymentHistoryEntity.AgentObservation transactionAgentObservation = new PaymentHistoryEntity.AgentObservation(
                "transaction-pattern-agent", true, 0.88, 0.77, "HIGH", 12.5, List.of(featureContribution), 0.05);
        return new PaymentHistoryEntity.MarlAssessment(
                "marl-req-1", "BLOCK", 0.91, 0.42, transactionAgentObservation, null, null,
                Map.of("transaction", 1.0), 34L, "inference");
    }

    private static PaymentSnapshotEvent.Builder newPaymentSnapshotEventBuilder(UUID paymentId, UUID customerId) {
        return PaymentSnapshotEvent.newBuilder()
                .setPaymentId(paymentId.toString())
                .setReferenceNumber("PAY-" + paymentId.toString().substring(0, 8).toUpperCase())
                .setCustomerId(customerId.toString())
                .setSourceAccountId(SOURCE_ACCOUNT_ID.toString())
                .setDestinationAccountId(DESTINATION_ACCOUNT_ID.toString())
                .setPaymentType("TRANSFER_OUT")
                .setPaymentScheme(PaymentScheme.INTERNAL_TRANSFER)
                .setAmount("100.00")
                .setFromCurrency("GBP")
                .setToCurrency("EUR")
                .setConvertedAmount("116.00")
                .setAppliedExchangeRate("1.16000000")
                .setDescription("Rent")
                .setInitiatedAt(INITIATED_AT.toEpochMilli())
                .setVersion(0)
                .setSnapshotTimestamp(INITIATED_AT.toEpochMilli())
                .setEventTrigger("PAYMENT_INITIATED");
    }
}
