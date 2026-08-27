package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class PaymentHistoryResponseMapper {

    public static PaymentHistoryResponse toPaymentHistoryResponse(
            PaymentHistoryEntity paymentHistoryEntity) {

        return PaymentHistoryResponse.builder()
                .paymentId(paymentHistoryEntity.getPaymentId())
                .referenceNumber(paymentHistoryEntity.getReferenceNumber())
                .customerId(paymentHistoryEntity.getCustomerId())
                .sourceAccountId(paymentHistoryEntity.getSourceAccountId())
                .destinationAccountId(paymentHistoryEntity.getDestinationAccountId())
                .amount(paymentHistoryEntity.getAmount())
                .fromCurrency(paymentHistoryEntity.getFromCurrency())
                .toCurrency(paymentHistoryEntity.getToCurrency())
                .convertedAmount(paymentHistoryEntity.getConvertedAmount())
                .appliedExchangeRate(paymentHistoryEntity.getAppliedExchangeRate())
                .paymentType(paymentHistoryEntity.getPaymentType())
                .description(paymentHistoryEntity.getDescription())
                .status(paymentHistoryEntity.getStatus())
                .fraudStatus(paymentHistoryEntity.getFraudStatus())
                .riskScore(paymentHistoryEntity.getRiskScore())
                .riskLevel(paymentHistoryEntity.getRiskLevel())
                .riskAction(paymentHistoryEntity.getRiskAction())
                .fraudIndicators(paymentHistoryEntity.getFraudIndicators())
                .marlAssessment(mapMarlAssessment(paymentHistoryEntity.getMarlAssessment()))
                .initiatedAt(paymentHistoryEntity.getInitiatedAt())
                .riskCheckRequestedAt(paymentHistoryEntity.getRiskCheckRequestedAt())
                .riskCheckCompletedAt(paymentHistoryEntity.getRiskCheckCompletedAt())
                .fraudCheckApprovedAt(paymentHistoryEntity.getFraudCheckApprovedAt())
                .manualReviewRequestedAt(paymentHistoryEntity.getManualReviewRequestedAt())
                .manualReviewApprovedAt(paymentHistoryEntity.getManualReviewApprovedAt())
                .manualReviewRejectedAt(paymentHistoryEntity.getManualReviewRejectedAt())
                .ledgerAuthorisationInitiatedAt(paymentHistoryEntity.getLedgerAuthorisationInitiatedAt())
                .ledgerAuthorisedAt(paymentHistoryEntity.getLedgerAuthorisedAt())
                .ledgerSettlementInitiatedAt(paymentHistoryEntity.getLedgerSettlementInitiatedAt())
                .ledgerSettledAt(paymentHistoryEntity.getLedgerSettledAt())
                .ledgerReleaseInitiatedAt(paymentHistoryEntity.getLedgerReleaseInitiatedAt())
                .ledgerReleasedAt(paymentHistoryEntity.getLedgerReleasedAt())
                .completedAt(paymentHistoryEntity.getCompletedAt())
                .blockedAt(paymentHistoryEntity.getBlockedAt())
                .manualReviewedBy(paymentHistoryEntity.getManualReviewedBy())
                .manualReviewNotes(paymentHistoryEntity.getManualReviewNotes())
                .blockReason(paymentHistoryEntity.getBlockReason())
                .failureReason(paymentHistoryEntity.getFailureReason())
                .decisionOverriddenBy(paymentHistoryEntity.getDecisionOverriddenBy())
                .decisionOverrideReason(paymentHistoryEntity.getDecisionOverrideReason())
                .decisionOverriddenAt(paymentHistoryEntity.getDecisionOverriddenAt())
                .riskProcessingTimeMs(paymentHistoryEntity.getRiskProcessingTimeMs())
                .marlProcessingTimeMs(paymentHistoryEntity.getMarlProcessingTimeMs())
                .mlModelVersion(paymentHistoryEntity.getMlModelVersion())
                .aggregateVersion(paymentHistoryEntity.getAggregateVersion())
                .createdAt(paymentHistoryEntity.getCreatedAt())
                .updatedAt(paymentHistoryEntity.getUpdatedAt())
                .build();
    }

    private static PaymentHistoryResponse.MarlAssessmentDto mapMarlAssessment(
            PaymentHistoryEntity.MarlAssessment marlAssessment) {

        if (marlAssessment == null) {
            return null;
        }

        return PaymentHistoryResponse.MarlAssessmentDto.builder()
                .requestId(marlAssessment.getRequestId())
                .action(marlAssessment.getAction())
                .confidence(marlAssessment.getConfidence())
                .maddpgQValue(marlAssessment.getMaddpgQValue())
                .transactionAgentObservation(mapAgentObservation(marlAssessment.getTransactionAgentObservation()))
                .customerAgentObservation(mapAgentObservation(marlAssessment.getCustomerAgentObservation()))
                .networkAgentObservation(mapAgentObservation(marlAssessment.getNetworkAgentObservation()))
                .agentContributions(marlAssessment.getAgentContributions())
                .processingTimeMs(marlAssessment.getProcessingTimeMs())
                .mode(marlAssessment.getMode())
                .build();
    }

    private static PaymentHistoryResponse.AgentObservationDto mapAgentObservation(
            PaymentHistoryEntity.AgentObservation agentObservation) {

        if (agentObservation == null) {
            return null;
        }

        return PaymentHistoryResponse.AgentObservationDto.builder()
                .agentName(agentObservation.getAgentName())
                .isSuspicious(agentObservation.getIsSuspicious())
                .probability(agentObservation.getProbability())
                .riskScore(agentObservation.getRiskScore())
                .confidence(agentObservation.getConfidence())
                .responseTimeMs(agentObservation.getResponseTimeMs())
                .featureContributions(mapFeatureContributions(agentObservation.getFeatureContributions()))
                .shapBaseValue(agentObservation.getShapBaseValue())
                .build();
    }

    private static List<PaymentHistoryResponse.FeatureContributionDto> mapFeatureContributions(
            List<PaymentHistoryEntity.FeatureContribution> featureContributions) {

        if (featureContributions == null) {
            return Collections.emptyList();
        }

        return featureContributions.stream()
                .map(featureContribution -> PaymentHistoryResponse.FeatureContributionDto.builder()
                        .feature(featureContribution.getFeature())
                        .value(featureContribution.getValue())
                        .shapValue(featureContribution.getShapValue())
                        .direction(featureContribution.getDirection())
                        .build())
                .toList();
    }
}
