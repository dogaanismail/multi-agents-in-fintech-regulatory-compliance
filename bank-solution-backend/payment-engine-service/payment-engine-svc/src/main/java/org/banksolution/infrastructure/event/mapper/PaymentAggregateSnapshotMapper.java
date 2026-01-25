package org.banksolution.infrastructure.event.mapper;

import com.aml.payment.*;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.MarlAssessment;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;

import java.time.Instant;

@UtilityClass
public class PaymentAggregateSnapshotMapper {

    public static PaymentSnapshotEvent toSnapshot(PaymentResponse payment, String eventTrigger) {
        return PaymentSnapshotEvent.newBuilder()
                .setPaymentId(payment.paymentId())
                .setReferenceNumber(payment.referenceNumber())
                .setCustomerId(payment.customerId())
                .setSourceAccountId(payment.sourceAccountId())
                .setDestinationAccountId(payment.destinationAccountId())
                .setAmount(payment.amount().toString())
                .setCurrency(payment.currency())
                .setPaymentType(payment.paymentType().name())
                .setDescription(payment.description())
                .setStatus(mapPaymentStatus(payment.status()))
                .setFraudStatus(mapFraudStatus(payment.fraudStatus()))
                .setRiskAssessment(mapRiskAssessment(payment.riskAssessment()))
                .setEventTrigger(eventTrigger)
                .setSnapshotTimestamp(Instant.now().toEpochMilli())
                .setVersion(0) //TODO: Check version for axon

                // Lifecycle timestamps
                .setInitiatedAt(toEpochMillis(payment.initiatedAt()))
                .setRiskCheckRequestedAt(toEpochMillis(payment.riskAssessmentRequestedAt()))
                .setRiskCheckCompletedAt(toEpochMillis(payment.riskAssessmentCompletedAt()))
                .setFraudCheckApprovedAt(toEpochMillis(payment.fraudCheckApprovedAt()))
                .setManualReviewRequestedAt(toEpochMillis(payment.manualReviewRequestedAt()))
                .setManualReviewApprovedAt(toEpochMillis(payment.manualReviewApprovedAt()))
                .setManualReviewRejectedAt(toEpochMillis(payment.manualReviewRejectedAt()))
                .setAccountChargeInitiatedAt(toEpochMillis(payment.accountChargeInitiatedAt()))
                .setAccountChargedAt(toEpochMillis(payment.accountChargedAt()))
                .setAccountChargeFailedAt(toEpochMillis(payment.accountChargeFailedAt()))
                .setCompletedAt(toEpochMillis(payment.completedAt()))
                .setBlockedAt(toEpochMillis(payment.blockedAt()))

                // Decision metadata
                .setManualReviewedBy(payment.manualReviewedBy())
                .setManualReviewNotes(payment.manualReviewNotes())
                .setBlockReason(payment.blockReason())
                .setFailureReason(payment.failureReason())

                .build();
    }

    private static com.aml.payment.PaymentStatus mapPaymentStatus(PaymentStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case INITIATED -> com.aml.payment.PaymentStatus.INITIATED;
            case FRAUD_CHECK_PENDING -> com.aml.payment.PaymentStatus.FRAUD_CHECK_PENDING;
            case FRAUD_CHECK_APPROVED -> com.aml.payment.PaymentStatus.FRAUD_CHECK_APPROVED;
            case FRAUD_CHECK_FAILED -> com.aml.payment.PaymentStatus.FRAUD_CHECK_FAILED;
            case MANUAL_REVIEW_REQUIRED -> com.aml.payment.PaymentStatus.MANUAL_REVIEW_REQUIRED;
            case ACCOUNT_CHARGE_PENDING -> com.aml.payment.PaymentStatus.ACCOUNT_CHARGE_PENDING;
            case ACCOUNT_CHARGED -> com.aml.payment.PaymentStatus.ACCOUNT_CHARGED;
            case COMPLETED -> com.aml.payment.PaymentStatus.COMPLETED;
            case BLOCKED -> com.aml.payment.PaymentStatus.BLOCKED;
            case FAILED -> com.aml.payment.PaymentStatus.FAILED;
        };
    }

    private static FraudCheckStatus mapFraudStatus(FraudAnalysisStatus fraudStatus) {
        if (fraudStatus == null) {
            return null;
        }
        return switch (fraudStatus) {
            case PENDING -> FraudCheckStatus.PENDING;
            case APPROVED -> FraudCheckStatus.APPROVED;
            case BLOCKED -> FraudCheckStatus.BLOCKED;
            case REVIEW_REQUIRED -> FraudCheckStatus.REVIEW_REQUIRED;
            case FAILED -> FraudCheckStatus.FAILED;
        };
    }

    private static RiskAssessmentSnapshot mapRiskAssessment(RiskAssessment risk) {
        if (risk == null) {
            return null;
        }

        return RiskAssessmentSnapshot.newBuilder()
                .setRiskScore(risk.riskScore())
                .setRiskLevel(risk.riskLevel())
                .setRiskAction(risk.riskAction())
                .setFraudIndicators(risk.fraudIndicators())
                .setMlModelVersion(risk.mlModelVersion())
                .setProcessingTimeMs(risk.processingTimeMs())
                .setMarlAssessment(mapMarlAssessment(risk.marlAssessment()))
                .build();
    }

    private static MarlAssessmentSnapshot mapMarlAssessment(MarlAssessment marl) {
        if (marl == null) {
            return null;
        }

        return MarlAssessmentSnapshot.newBuilder()
                .setRequestId(marl.requestId())
                .setAction(marl.action())
                .setConfidence(marl.confidence())
                .setMaddpgQValue(marl.maddpgQValue())
                .setTransactionAgentObservation(mapTransactionAgentObservation(marl.transactionAgentObservation()))
                .setCustomerAgentObservation(mapCustomerAgentObservation(marl.customerAgentObservation()))
                .setNetworkAgentObservation(mapNetworkAgentObservation(marl.networkAgentObservation()))
                .setAgentContributions(marl.agentContributions())
                .setProcessingTimeMs(marl.processingTimeMs().doubleValue())
                .setMode(marl.mode())
                .build();
    }

    private static TransactionAgentObservationSnapshot mapTransactionAgentObservation(AgentObservation obs) {
        if (obs == null) {
            return null;
        }

        return TransactionAgentObservationSnapshot.newBuilder()
                .setAgentName(obs.agentName())
                .setIsSuspicious(obs.isSuspicious())
                .setProbability(obs.probability())
                .setRiskScore(obs.riskScore())
                .setConfidence(obs.confidence())
                .setResponseTimeMs(obs.responseTimeMs())
                .build();
    }

    private static CustomerAgentObservationSnapshot mapCustomerAgentObservation(AgentObservation obs) {
        if (obs == null) {
            return null;
        }

        return CustomerAgentObservationSnapshot.newBuilder()
                .setAgentName(obs.agentName())
                .setIsSuspicious(obs.isSuspicious())
                .setProbability(obs.probability())
                .setRiskScore(obs.riskScore())
                .setConfidence(obs.confidence())
                .setResponseTimeMs(obs.responseTimeMs())
                .build();
    }

    private static NetworkAgentObservationSnapshot mapNetworkAgentObservation(AgentObservation obs) {
        if (obs == null) {
            return null;
        }

        return NetworkAgentObservationSnapshot.newBuilder()
                .setAgentName(obs.agentName())
                .setIsSuspicious(obs.isSuspicious())
                .setProbability(obs.probability())
                .setRiskScore(obs.riskScore())
                .setConfidence(obs.confidence())
                .setResponseTimeMs(obs.responseTimeMs())
                .build();
    }

    private static Long toEpochMillis(Instant instant) {
        return instant != null ? instant.toEpochMilli() : null;
    }
}
