package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEventWithMarl;

class RiskAssessmentMapperTest {

    @Test
    void shouldMapEveryFieldAndTurnEnumsIntoTheirNames() {
        com.aml.risk.RiskAssessmentCompletedEvent riskAssessmentCompletedEvent =
                createRiskAssessmentCompletedEvent(RiskAction.ESCALATE, RiskLevel.MEDIUM, 0.6);

        RiskAssessment riskAssessment = RiskAssessmentMapper.toRiskAssessment(riskAssessmentCompletedEvent);

        assertThat(riskAssessment.riskCheckRequestId()).isEqualTo(riskAssessmentCompletedEvent.getRiskCheckRequestId());
        assertThat(riskAssessment.riskScore()).isEqualTo(0.6);
        assertThat(riskAssessment.riskLevel()).isEqualTo("MEDIUM");
        assertThat(riskAssessment.riskAction()).isEqualTo("ESCALATE");
        assertThat(riskAssessment.fraudIndicators()).containsExactly("NONE");
        assertThat(riskAssessment.mlModelVersion()).isEqualTo("model-v1");
        assertThat(riskAssessment.processingTimeMs()).isEqualTo(12L);
        assertThat(riskAssessment.marlAssessment()).isNull();
    }

    @Test
    void shouldMapTheMarlAssessmentWhenPresent() {
        RiskAssessment riskAssessment =
                RiskAssessmentMapper.toRiskAssessment(createRiskAssessmentCompletedEventWithMarl(RiskAction.BLOCK));

        assertThat(riskAssessment.marlAssessment()).isNotNull();
        assertThat(riskAssessment.marlAssessment().action()).isEqualTo("BLOCK");
    }
}
