package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.MarlAssessment;
import org.banksolution.domain.payment.valueobject.AgentObservation;
import org.banksolution.domain.payment.valueobject.FeatureContribution;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AvroEventFixtures.createMarlAssessment;

class AgentObservationMappersTest {

    private final MarlAssessment marlAssessment = createMarlAssessment();

    @Test
    void shouldMapTheTransactionAgentIncludingItsFeatureContributions() {
        AgentObservation agentObservation =
                TransactionAgentObservationMapper.toDomain(marlAssessment.getTransactionAgentObservation());

        assertThat(agentObservation.agentName()).isEqualTo("transaction-pattern-agent");
        assertThat(agentObservation.isSuspicious()).isTrue();
        assertThat(agentObservation.probability()).isEqualTo(0.88);
        assertThat(agentObservation.riskScore()).isEqualTo(0.77);
        assertThat(agentObservation.confidence()).isEqualTo("HIGH");
        assertThat(agentObservation.responseTimeMs()).isEqualTo(12.5);
        assertThat(agentObservation.shapBaseValue()).isEqualTo(0.05);
        assertThat(agentObservation.featureContributions())
                .containsExactly(new FeatureContribution("amount", "100.00", 0.31, "increase"));
    }

    @Test
    void shouldKeepAbsentFeatureContributionsAbsentForTheCustomerAgent() {
        AgentObservation agentObservation =
                CustomerAgentObservationMapper.toDomain(marlAssessment.getCustomerAgentObservation());

        assertThat(agentObservation.agentName()).isEqualTo("customer-risk-agent");
        assertThat(agentObservation.isSuspicious()).isFalse();
        assertThat(agentObservation.featureContributions()).isNull();
        assertThat(agentObservation.shapBaseValue()).isNull();
    }

    @Test
    void shouldMapAnEmptyFeatureListForTheNetworkAgent() {
        AgentObservation agentObservation =
                NetworkAgentObservationMapper.toDomain(marlAssessment.getNetworkAgentObservation());

        assertThat(agentObservation.agentName()).isEqualTo("network-analysis-agent");
        assertThat(agentObservation.featureContributions()).isEmpty();
        assertThat(agentObservation.shapBaseValue()).isEqualTo(0.1);
    }

    @Test
    void shouldMapTheWholeMarlAssessment() {
        org.banksolution.domain.payment.valueobject.MarlAssessment domainMarlAssessment =
                MarlAssessmentMapper.toDomain(marlAssessment);

        assertThat(domainMarlAssessment.requestId()).isEqualTo("marl-req-1");
        assertThat(domainMarlAssessment.action()).isEqualTo("BLOCK");
        assertThat(domainMarlAssessment.confidence()).isEqualTo(0.91);
        assertThat(domainMarlAssessment.maddpgQValue()).isEqualTo(0.42);
        assertThat(domainMarlAssessment.agentContributions()).containsEntry("transaction", 0.5).hasSize(3);
        assertThat(domainMarlAssessment.processingTimeMs()).isEqualTo(34L);
        assertThat(domainMarlAssessment.mode()).isEqualTo("inference");
        assertThat(domainMarlAssessment.transactionAgentObservation().agentName()).isEqualTo("transaction-pattern-agent");
        assertThat(domainMarlAssessment.customerAgentObservation().agentName()).isEqualTo("customer-risk-agent");
        assertThat(domainMarlAssessment.networkAgentObservation().agentName()).isEqualTo("network-analysis-agent");
    }
}
