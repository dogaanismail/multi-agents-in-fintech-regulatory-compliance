package org.banksolution.mapper;

import com.aml.payment.CustomerAgentObservationSnapshot;
import com.aml.payment.NetworkAgentObservationSnapshot;
import com.aml.payment.TransactionAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.List;

@UtilityClass
public class AgentObservationSnapshotMapper {

    public static PaymentHistoryEntity.AgentObservation mapTransactionAgentObservation(
            TransactionAgentObservationSnapshot source) {

        return getAgentObservation(source.getAgentName(),
                source.getIsSuspicious(),
                source.getProbability(),
                source.getRiskScore(),
                source.getConfidence(),
                source.getResponseTimeMs(),
                mapFeatureContributions(source.getFeatureContributions()),
                source.getShapBaseValue()
        );
    }

    public static PaymentHistoryEntity.AgentObservation mapCustomerAgentObservation(
            CustomerAgentObservationSnapshot source) {

        return getAgentObservation(source.getAgentName(),
                source.getIsSuspicious(),
                source.getProbability(),
                source.getRiskScore(),
                source.getConfidence(),
                source.getResponseTimeMs(),
                mapFeatureContributions(source.getFeatureContributions()),
                source.getShapBaseValue()
        );
    }

    public static PaymentHistoryEntity.AgentObservation mapNetworkAgentObservation(
            NetworkAgentObservationSnapshot source) {

        return getAgentObservation(source.getAgentName(),
                source.getIsSuspicious(),
                source.getProbability(),
                source.getRiskScore(),
                source.getConfidence(),
                source.getResponseTimeMs(),
                mapFeatureContributions(source.getFeatureContributions()),
                source.getShapBaseValue()
        );
    }

    private static PaymentHistoryEntity.AgentObservation getAgentObservation(
            String agentName,
            boolean isSuspicious,
            double probability,
            double riskScore,
            String confidence,
            double responseTimeMs,
            java.util.List<PaymentHistoryEntity.FeatureContribution> featureContributions,
            Double shapBaseValue) {

        PaymentHistoryEntity.AgentObservation observation = new PaymentHistoryEntity.AgentObservation();
        observation.setAgentName(agentName);
        observation.setIsSuspicious(isSuspicious);
        observation.setProbability(probability);
        observation.setRiskScore(riskScore);
        observation.setConfidence(confidence);
        observation.setResponseTimeMs(responseTimeMs);
        observation.setFeatureContributions(featureContributions);
        observation.setShapBaseValue(shapBaseValue);

        return observation;
    }

    private static List<PaymentHistoryEntity.FeatureContribution> mapFeatureContributions(
            List<com.aml.payment.FeatureContribution> featureContributions) {

        if (featureContributions == null) {
            return null;
        }

        return featureContributions.stream()
                .map(featureContribution -> new PaymentHistoryEntity.FeatureContribution(
                        featureContribution.getFeature(),
                        featureContribution.getValue(),
                        featureContribution.getShapValue(),
                        featureContribution.getDirection()))
                .toList();
    }
}
