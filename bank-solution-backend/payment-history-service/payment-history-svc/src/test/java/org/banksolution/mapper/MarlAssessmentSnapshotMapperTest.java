package org.banksolution.mapper;

import org.banksolution.entity.PaymentHistoryEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentHistoryFixtures.createMarlAssessmentSnapshot;

class MarlAssessmentSnapshotMapperTest {

    @Test
    void shouldMapEveryAgentObservationAndTheContributions() {
        PaymentHistoryEntity.MarlAssessment marlAssessment =
                MarlAssessmentSnapshotMapper.mapMarlAssessment(createMarlAssessmentSnapshot());

        assertThat(marlAssessment.getRequestId()).isEqualTo("marl-req-1");
        assertThat(marlAssessment.getAction()).isEqualTo("BLOCK");
        assertThat(marlAssessment.getConfidence()).isEqualTo(0.91);
        assertThat(marlAssessment.getMaddpgQValue()).isEqualTo(0.42);
        assertThat(marlAssessment.getProcessingTimeMs()).isEqualTo(34L);
        assertThat(marlAssessment.getMode()).isEqualTo("inference");
        assertThat(marlAssessment.getAgentContributions()).containsEntry("network", 0.2).hasSize(3);
        assertThat(marlAssessment.getTransactionAgentObservation().getAgentName()).isEqualTo("transaction-pattern-agent");
        assertThat(marlAssessment.getTransactionAgentObservation().getFeatureContributions())
                .singleElement()
                .satisfies(featureContribution -> {
                    assertThat(featureContribution.getFeature()).isEqualTo("amount");
                    assertThat(featureContribution.getShapValue()).isEqualTo(0.31);
                    assertThat(featureContribution.getDirection()).isEqualTo("increase");
                });
        assertThat(marlAssessment.getCustomerAgentObservation().getFeatureContributions()).isNull();
        assertThat(marlAssessment.getCustomerAgentObservation().getShapBaseValue()).isNull();
        assertThat(marlAssessment.getNetworkAgentObservation().getFeatureContributions()).isEmpty();
        assertThat(marlAssessment.getNetworkAgentObservation().getShapBaseValue()).isEqualTo(0.1);
    }
}
