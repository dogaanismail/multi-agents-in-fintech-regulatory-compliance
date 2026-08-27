package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.*;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.MarlAssessment;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;

import java.time.Instant;
import java.util.Collections;

@UtilityClass
public class PaymentAggregateSnapshotMapper {

    public static PaymentSnapshotEvent toSnapshot(PaymentResponse paymentResponse, String eventTrigger) {
        return PaymentSnapshotEvent.newBuilder()
                .setPaymentId(paymentResponse.paymentId())
                .setReferenceNumber(paymentResponse.referenceNumber())
                .setCustomerId(paymentResponse.customerId())
                .setSourceAccountId(paymentResponse.sourceAccountId())
                .setDestinationAccountId(paymentResponse.destinationAccountId())
                .setAmount(paymentResponse.amount().toString())
                .setFromCurrency(paymentResponse.fromCurrency())
                .setToCurrency(paymentResponse.toCurrency())
                .setConvertedAmount(paymentResponse.convertedAmount().toString())
                .setAppliedExchangeRate(paymentResponse.appliedExchangeRate() != null ? paymentResponse.appliedExchangeRate().toString() : null)
                .setPaymentScheme(mapPaymentScheme(paymentResponse.paymentScheme()))
                .setPaymentType(paymentResponse.paymentType().name())
                .setDescription(paymentResponse.description())
                .setStatus(mapPaymentStatus(paymentResponse.status()))
                .setFraudStatus(mapFraudStatus(paymentResponse.fraudStatus()))
                .setRiskAssessment(mapRiskAssessment(paymentResponse.riskAssessment()))
                .setEventTrigger(eventTrigger)
                .setSnapshotTimestamp(Instant.now().toEpochMilli())
                .setVersion(toSnapshotVersion(paymentResponse.version()))

                // Lifecycle timestamps
                .setInitiatedAt(toEpochMillis(paymentResponse.initiatedAt()))
                .setRiskCheckRequestedAt(toEpochMillis(paymentResponse.riskAssessmentRequestedAt()))
                .setRiskCheckCompletedAt(toEpochMillis(paymentResponse.riskAssessmentCompletedAt()))
                .setFraudCheckApprovedAt(toEpochMillis(paymentResponse.fraudCheckApprovedAt()))
                .setManualReviewRequestedAt(toEpochMillis(paymentResponse.manualReviewRequestedAt()))
                .setManualReviewApprovedAt(toEpochMillis(paymentResponse.manualReviewApprovedAt()))
                .setManualReviewRejectedAt(toEpochMillis(paymentResponse.manualReviewRejectedAt()))
                .setLedgerAuthorisationInitiatedAt(toEpochMillis(paymentResponse.ledgerAuthorisationInitiatedAt()))
                .setLedgerAuthorisedAt(toEpochMillis(paymentResponse.ledgerAuthorisedAt()))
                .setLedgerSettlementInitiatedAt(toEpochMillis(paymentResponse.ledgerSettlementInitiatedAt()))
                .setLedgerSettledAt(toEpochMillis(paymentResponse.ledgerSettledAt()))
                .setLedgerReleaseInitiatedAt(toEpochMillis(paymentResponse.ledgerReleaseInitiatedAt()))
                .setLedgerReleasedAt(toEpochMillis(paymentResponse.ledgerReleasedAt()))
                .setCompletedAt(toEpochMillis(paymentResponse.completedAt()))
                .setBlockedAt(toEpochMillis(paymentResponse.blockedAt()))

                // Decision metadata
                .setManualReviewedBy(paymentResponse.manualReviewedBy())
                .setManualReviewNotes(paymentResponse.manualReviewNotes())
                .setBlockReason(paymentResponse.blockReason())
                .setFailureReason(paymentResponse.failureReason())

                // Decision override metadata
                .setDecisionOverriddenBy(paymentResponse.decisionOverriddenBy())
                .setDecisionOverrideReason(paymentResponse.decisionOverrideReason())
                .setDecisionOverriddenAt(toEpochMillis(paymentResponse.decisionOverriddenAt()))

                .build();
    }

    private static com.aml.payment.PaymentScheme mapPaymentScheme(String paymentScheme) {
        return paymentScheme == null ? null : com.aml.payment.PaymentScheme.valueOf(paymentScheme);
    }

    private static com.aml.payment.PaymentStatus mapPaymentStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case INITIATED -> com.aml.payment.PaymentStatus.INITIATED;
            case AUTHORISATION_PENDING -> com.aml.payment.PaymentStatus.AUTHORISATION_PENDING;
            case AUTHORISED -> com.aml.payment.PaymentStatus.AUTHORISED;
            case AUTHORISATION_DECLINED -> com.aml.payment.PaymentStatus.AUTHORISATION_DECLINED;
            case FRAUD_CHECK_PENDING -> com.aml.payment.PaymentStatus.FRAUD_CHECK_PENDING;
            case FRAUD_CHECK_APPROVED -> com.aml.payment.PaymentStatus.FRAUD_CHECK_APPROVED;
            case FRAUD_CHECK_FAILED -> com.aml.payment.PaymentStatus.FRAUD_CHECK_FAILED;
            case MANUAL_REVIEW_REQUIRED -> com.aml.payment.PaymentStatus.MANUAL_REVIEW_REQUIRED;
            case SETTLEMENT_PENDING -> com.aml.payment.PaymentStatus.SETTLEMENT_PENDING;
            case SETTLED -> com.aml.payment.PaymentStatus.SETTLED;
            case RELEASE_PENDING -> com.aml.payment.PaymentStatus.RELEASE_PENDING;
            case RELEASED -> com.aml.payment.PaymentStatus.RELEASED;
            case COMPLETED -> com.aml.payment.PaymentStatus.COMPLETED;
            case BLOCKED -> com.aml.payment.PaymentStatus.BLOCKED;
            case FAILED -> com.aml.payment.PaymentStatus.FAILED;
            case OVERRIDE_APPROVED -> com.aml.payment.PaymentStatus.OVERRIDE_APPROVED;
            case OVERRIDE_REJECTED -> com.aml.payment.PaymentStatus.OVERRIDE_REJECTED;
        };
    }

    private static FraudCheckStatus mapFraudStatus(FraudAnalysisStatus fraudAnalysisStatus) {
        return switch (fraudAnalysisStatus) {
            case PENDING -> FraudCheckStatus.PENDING;
            case APPROVED -> FraudCheckStatus.APPROVED;
            case BLOCKED -> FraudCheckStatus.BLOCKED;
            case REVIEW_REQUIRED -> FraudCheckStatus.REVIEW_REQUIRED;
            case FAILED -> FraudCheckStatus.FAILED;
        };
    }

    private static RiskAssessmentSnapshot mapRiskAssessment(RiskAssessment riskAssessment) {
        if (riskAssessment == null) {
            return null;
        }

        return RiskAssessmentSnapshot.newBuilder()
                .setRiskScore(riskAssessment.riskScore())
                .setRiskLevel(riskAssessment.riskLevel())
                .setRiskAction(riskAssessment.riskAction())
                .setFraudIndicators(riskAssessment.fraudIndicators())
                .setMlModelVersion(riskAssessment.mlModelVersion())
                .setProcessingTimeMs(riskAssessment.processingTimeMs())
                .setMarlAssessment(mapMarlAssessment(riskAssessment.marlAssessment()))
                .build();
    }

    private static MarlAssessmentSnapshot mapMarlAssessment(MarlAssessment marlAssessment) {
        if (marlAssessment == null) {
            return null;
        }

        return MarlAssessmentSnapshot.newBuilder()
                .setRequestId(marlAssessment.requestId())
                .setAction(marlAssessment.action())
                .setConfidence(marlAssessment.confidence())
                .setMaddpgQValue(marlAssessment.maddpgQValue())
                .setTransactionAgentObservation(mapTransactionAgentObservation(marlAssessment.transactionAgentObservation()))
                .setCustomerAgentObservation(mapCustomerAgentObservation(marlAssessment.customerAgentObservation()))
                .setNetworkAgentObservation(mapNetworkAgentObservation(marlAssessment.networkAgentObservation()))
                .setAgentContributions(marlAssessment.agentContributions())
                .setProcessingTimeMs(marlAssessment.processingTimeMs().doubleValue())
                .setMode(marlAssessment.mode())
                .build();
    }

    private static TransactionAgentObservationSnapshot mapTransactionAgentObservation(AgentObservation agentObservation) {
        return TransactionAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.agentName())
                .setIsSuspicious(agentObservation.isSuspicious())
                .setProbability(agentObservation.probability())
                .setRiskScore(agentObservation.riskScore())
                .setConfidence(agentObservation.confidence())
                .setResponseTimeMs(agentObservation.responseTimeMs())
                .setFeatureContributions(mapFeatureContributions(agentObservation.featureContributions()))
                .setShapBaseValue(agentObservation.shapBaseValue())
                .build();
    }

    private static CustomerAgentObservationSnapshot mapCustomerAgentObservation(AgentObservation agentObservation) {
        return CustomerAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.agentName())
                .setIsSuspicious(agentObservation.isSuspicious())
                .setProbability(agentObservation.probability())
                .setRiskScore(agentObservation.riskScore())
                .setConfidence(agentObservation.confidence())
                .setResponseTimeMs(agentObservation.responseTimeMs())
                .setFeatureContributions(mapFeatureContributions(agentObservation.featureContributions()))
                .setShapBaseValue(agentObservation.shapBaseValue())
                .build();
    }

    private static NetworkAgentObservationSnapshot mapNetworkAgentObservation(AgentObservation agentObservation) {
        return NetworkAgentObservationSnapshot.newBuilder()
                .setAgentName(agentObservation.agentName())
                .setIsSuspicious(agentObservation.isSuspicious())
                .setProbability(agentObservation.probability())
                .setRiskScore(agentObservation.riskScore())
                .setConfidence(agentObservation.confidence())
                .setResponseTimeMs(agentObservation.responseTimeMs())
                .setFeatureContributions(mapFeatureContributions(agentObservation.featureContributions()))
                .setShapBaseValue(agentObservation.shapBaseValue())
                .build();
    }

    private static int toSnapshotVersion(Long aggregateVersion) {
        return aggregateVersion != null ? aggregateVersion.intValue() : 0;
    }

    private static Long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }

    private static java.util.List<com.aml.payment.FeatureContribution> mapFeatureContributions(
            java.util.List<org.banksolution.domain.payment.valueobject.FeatureContribution> featureContributions) {

        if (featureContributions == null) {
            return Collections.emptyList();
        }

        return featureContributions.stream()
                .map(featureContribution -> com.aml.payment.FeatureContribution.newBuilder()
                        .setFeature(featureContribution.feature())
                        .setValue(featureContribution.value())
                        .setShapValue(featureContribution.shapValue())
                        .setDirection(featureContribution.direction())
                        .build())
                .toList();
    }
}
