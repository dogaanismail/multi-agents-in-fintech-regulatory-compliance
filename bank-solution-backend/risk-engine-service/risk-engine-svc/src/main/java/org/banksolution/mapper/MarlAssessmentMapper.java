package org.banksolution.mapper;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.MarlAction;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.AgentObservationEntity;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.AgentType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@UtilityClass
public class MarlAssessmentMapper {

    public static MarlAssessmentEntity toMarlAssessmentEntity(
            FraudAnalysisCompletedEvent event,
            RiskCheckRequestEntity riskCheckRequest) {

        return MarlAssessmentEntity.builder()
                .riskCheckRequest(riskCheckRequest)
                .action(MarlAction.valueOf(event.getAction().name()))
                .confidence(BigDecimal.valueOf(event.getConfidence()))
                .maddpgQValue(BigDecimal.valueOf(event.getMaddpgQValue()))
                .processingTimeMs(BigDecimal.valueOf(event.getProcessingTimeMs()))
                .mode(event.getMode())
                .responseTimestamp(event.getTimestamp())
                .build();
    }

    public static List<AgentObservationEntity> toAgentObservations(
            FraudAnalysisCompletedEvent event,
            MarlAssessmentEntity marlAssessment) {

        Map<String, Double> contributions = event.getAgentContributions();

        return List.of(
                toAgentObservation(
                        event.getTransactionAgentObservation().getAgentName(),
                        event.getTransactionAgentObservation().getIsSuspicious(),
                        event.getTransactionAgentObservation().getProbability(),
                        event.getTransactionAgentObservation().getRiskScore(),
                        event.getTransactionAgentObservation().getConfidence(),
                        event.getTransactionAgentObservation().getResponseTimeMs(),
                        AgentType.TRANSACTION,
                        marlAssessment,
                        contributions.get("transaction")
                ),
                toAgentObservation(
                        event.getCustomerAgentObservation().getAgentName(),
                        event.getCustomerAgentObservation().getIsSuspicious(),
                        event.getCustomerAgentObservation().getProbability(),
                        event.getCustomerAgentObservation().getRiskScore(),
                        event.getCustomerAgentObservation().getConfidence(),
                        event.getCustomerAgentObservation().getResponseTimeMs(),
                        AgentType.CUSTOMER,
                        marlAssessment,
                        contributions.get("customer")
                ),
                toAgentObservation(
                        event.getNetworkAgentObservation().getAgentName(),
                        event.getNetworkAgentObservation().getIsSuspicious(),
                        event.getNetworkAgentObservation().getProbability(),
                        event.getNetworkAgentObservation().getRiskScore(),
                        event.getNetworkAgentObservation().getConfidence(),
                        event.getNetworkAgentObservation().getResponseTimeMs(),
                        AgentType.NETWORK,
                        marlAssessment,
                        contributions.get("network")
                )
        );
    }

    private static AgentObservationEntity toAgentObservation(
            String agentName,
            Boolean isSuspicious,
            Double probability,
            Double riskScore,
            String confidence,
            Double responseTimeMs,
            AgentType agentType,
            MarlAssessmentEntity marlAssessment,
            Double contribution) {

        return AgentObservationEntity.builder()
                .marlAssessment(marlAssessment)
                .agentName(agentName)
                .agentType(agentType)
                .isSuspicious(isSuspicious)
                .probability(BigDecimal.valueOf(probability))
                .riskScore(BigDecimal.valueOf(riskScore))
                .confidence(confidence)
                .responseTimeMs(BigDecimal.valueOf(responseTimeMs))
                .contribution(contribution != null ? BigDecimal.valueOf(contribution) : null)
                .build();
    }
}