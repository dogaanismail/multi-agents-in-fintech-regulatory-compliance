package org.banksolution.mapper;

import com.aml.payment.CustomerAgentObservationSnapshot;
import com.aml.payment.NetworkAgentObservationSnapshot;
import com.aml.payment.TransactionAgentObservationSnapshot;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.PaymentHistoryEntity;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class AgentObservationSnapshotMapper {

    public static PaymentHistoryEntity.AgentObservation mapTransactionAgentObservation(
            TransactionAgentObservationSnapshot transactionAgentObservationSnapshot) {

        return toAgentObservation(transactionAgentObservationSnapshot.getAgentName(),
                transactionAgentObservationSnapshot.getIsSuspicious(),
                transactionAgentObservationSnapshot.getProbability(),
                transactionAgentObservationSnapshot.getRiskScore(),
                transactionAgentObservationSnapshot.getConfidence(),
                transactionAgentObservationSnapshot.getResponseTimeMs(),
                mapFeatureContributions(transactionAgentObservationSnapshot.getFeatureContributions()),
                transactionAgentObservationSnapshot.getShapBaseValue()
        );
    }

    public static PaymentHistoryEntity.AgentObservation mapCustomerAgentObservation(
            CustomerAgentObservationSnapshot customerAgentObservationSnapshot) {

        return toAgentObservation(customerAgentObservationSnapshot.getAgentName(),
                customerAgentObservationSnapshot.getIsSuspicious(),
                customerAgentObservationSnapshot.getProbability(),
                customerAgentObservationSnapshot.getRiskScore(),
                customerAgentObservationSnapshot.getConfidence(),
                customerAgentObservationSnapshot.getResponseTimeMs(),
                mapFeatureContributions(customerAgentObservationSnapshot.getFeatureContributions()),
                customerAgentObservationSnapshot.getShapBaseValue()
        );
    }

    public static PaymentHistoryEntity.AgentObservation mapNetworkAgentObservation(
            NetworkAgentObservationSnapshot networkAgentObservationSnapshot) {

        return toAgentObservation(networkAgentObservationSnapshot.getAgentName(),
                networkAgentObservationSnapshot.getIsSuspicious(),
                networkAgentObservationSnapshot.getProbability(),
                networkAgentObservationSnapshot.getRiskScore(),
                networkAgentObservationSnapshot.getConfidence(),
                networkAgentObservationSnapshot.getResponseTimeMs(),
                mapFeatureContributions(networkAgentObservationSnapshot.getFeatureContributions()),
                networkAgentObservationSnapshot.getShapBaseValue()
        );
    }

    private static PaymentHistoryEntity.AgentObservation toAgentObservation(
            String agentName,
            boolean isSuspicious,
            double probability,
            double riskScore,
            String confidence,
            double responseTimeMs,
            List<PaymentHistoryEntity.FeatureContribution> featureContributions,
            Double shapBaseValue) {

        PaymentHistoryEntity.AgentObservation agentObservation = new PaymentHistoryEntity.AgentObservation();
        agentObservation.setAgentName(agentName);
        agentObservation.setIsSuspicious(isSuspicious);
        agentObservation.setProbability(probability);
        agentObservation.setRiskScore(riskScore);
        agentObservation.setConfidence(confidence);
        agentObservation.setResponseTimeMs(responseTimeMs);
        agentObservation.setFeatureContributions(featureContributions);
        agentObservation.setShapBaseValue(shapBaseValue);
        return agentObservation;
    }

    private static List<PaymentHistoryEntity.FeatureContribution> mapFeatureContributions(List<com.aml.payment.FeatureContribution> featureContributions) {

        if (featureContributions == null) {
            return Collections.emptyList();
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
