package org.banksolution.infrastructure.messaging.kafka.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.FeatureContribution;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                // Stream.toList() yields a JDK-internal ImmutableCollections type, and the event
                // serializer bakes the class name into the stored event; keep it a plain ArrayList.
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
