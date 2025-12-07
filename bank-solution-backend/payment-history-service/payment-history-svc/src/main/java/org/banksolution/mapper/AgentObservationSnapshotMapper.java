package org.banksolution.mapper;

import com.aml.payment.CustomerAgentObservationSnapshot;
import com.aml.payment.NetworkAgentObservationSnapshot;
import com.aml.payment.TransactionAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

@UtilityClass
public class AgentObservationSnapshotMapper {

    public static PaymentHistoryEntity.AgentObservation mapTransactionAgentObservation(
            TransactionAgentObservationSnapshot source) {

        return getAgentObservation(source.getAgentName(),
                source.getIsSuspicious(),
                source.getProbability(),
                source.getRiskScore(),
                source.getConfidence(),
                source.getResponseTimeMs()
        );
    }

    public static PaymentHistoryEntity.AgentObservation mapCustomerAgentObservation(
            CustomerAgentObservationSnapshot source) {

        return getAgentObservation(source.getAgentName(),
                source.getIsSuspicious(),
                source.getProbability(),
                source.getRiskScore(),
                source.getConfidence(),
                source.getResponseTimeMs()
        );
    }

    public static PaymentHistoryEntity.AgentObservation mapNetworkAgentObservation(
            NetworkAgentObservationSnapshot source) {

        return getAgentObservation(source.getAgentName(),
                source.getIsSuspicious(),
                source.getProbability(),
                source.getRiskScore(),
                source.getConfidence(),
                source.getResponseTimeMs()
        );
    }

    private static PaymentHistoryEntity.AgentObservation getAgentObservation(
            String agentName,
            boolean isSuspicious,
            double probability,
            double riskScore,
            String confidence,
            double responseTimeMs) {

        PaymentHistoryEntity.AgentObservation observation = new PaymentHistoryEntity.AgentObservation();
        observation.setAgentName(agentName);
        observation.setIsSuspicious(isSuspicious);
        observation.setProbability(probability);
        observation.setRiskScore(riskScore);
        observation.setConfidence(confidence);
        observation.setResponseTimeMs(responseTimeMs);

        return observation;
    }
}
