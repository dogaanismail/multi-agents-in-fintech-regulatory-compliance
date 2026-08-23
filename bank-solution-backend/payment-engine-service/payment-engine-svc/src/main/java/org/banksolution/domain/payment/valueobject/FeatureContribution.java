package org.banksolution.domain.payment.valueobject;

public record FeatureContribution(
        String feature,
        String value,
        Double shapValue,
        String direction
) {
}
