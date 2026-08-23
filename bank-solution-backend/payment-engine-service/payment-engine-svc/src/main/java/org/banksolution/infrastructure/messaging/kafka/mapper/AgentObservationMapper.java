package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.FeatureContribution;

import java.util.List;

@UtilityClass
public class AgentObservationMapper {

    public AgentObservation toAgentObservation(
            String agentName,
            boolean isSuspicious,
            double probability,
            double riskScore,
            String confidence,
            double responseTimeMs,
            List<com.aml.risk.FeatureContribution> featureContributions,
            Double shapBaseValue) {

        return new AgentObservation(
                agentName,
                isSuspicious,
                probability,
                riskScore,
                confidence,
                responseTimeMs,
                toFeatureContributions(featureContributions),
                shapBaseValue
        );
    }

    private static List<FeatureContribution> toFeatureContributions(
            List<com.aml.risk.FeatureContribution> featureContributions) {

        if (featureContributions == null) {
            return null;
        }

        return featureContributions.stream()
                .map(featureContribution -> new FeatureContribution(
                        featureContribution.getFeature(),
                        featureContribution.getValue(),
                        featureContribution.getShapValue(),
                        featureContribution.getDirection()))
                .toList();
    }
}
