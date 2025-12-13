package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.dto.PaymentHistoryResponse;
import org.banksolution.entity.PaymentHistoryEntity;

@UtilityClass
public class PaymentHistoryResponseMapper {

    public static PaymentHistoryResponse toResponse(
            PaymentHistoryEntity history) {

        return PaymentHistoryResponse.builder()
                .paymentId(history.getPaymentId())
                .referenceNumber(history.getReferenceNumber())
                .customerId(history.getCustomerId())
                .sourceAccountId(history.getSourceAccountId())
                .destinationAccountId(history.getDestinationAccountId())
                .amount(history.getAmount())
                .currency(history.getCurrency())
                .paymentType(history.getPaymentType())
                .description(history.getDescription())
                .status(history.getStatus())
                .fraudStatus(history.getFraudStatus())
                .riskScore(history.getRiskScore())
                .riskLevel(history.getRiskLevel())
                .riskAction(history.getRiskAction())
                .fraudIndicators(history.getFraudIndicators())
                .marlAssessment(mapMarlAssessment(history.getMarlAssessment()))
                .initiatedAt(history.getInitiatedAt())
                .riskCheckRequestedAt(history.getRiskCheckRequestedAt())
                .riskCheckCompletedAt(history.getRiskCheckCompletedAt())
                .completedAt(history.getCompletedAt())
                .blockedAt(history.getBlockedAt())
                .riskProcessingTimeMs(history.getRiskProcessingTimeMs())
                .marlProcessingTimeMs(history.getMarlProcessingTimeMs())
                .mlModelVersion(history.getMlModelVersion())
                .aggregateVersion(history.getAggregateVersion())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
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
                .build();
    }
}
