package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.RiskAssessmentCompletedEvent;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskAssessmentCompletedEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createRiskAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentCompleteServiceTest {

    @Mock
    private RiskAssessmentCompletedEventProducer riskAssessmentCompletedEventProducer;

    @InjectMocks
    private RiskAssessmentCompleteService riskAssessmentCompleteService;

    @Test
    void shouldPublishTheCompletedEventBuiltFromTheAssessment() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        RiskAssessmentEntity riskAssessment = createRiskAssessmentEntity(riskCheckRequest);
        FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), "PAY-1");

        riskAssessmentCompleteService.publishRiskAssessmentCompletedEvent(
                fraudAnalysisCompletedEvent, riskCheckRequest, riskAssessment);

        ArgumentCaptor<RiskAssessmentCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(RiskAssessmentCompletedEvent.class);
        verify(riskAssessmentCompletedEventProducer).produceRiskAssessmentCompletedEvent(eventCaptor.capture());

        RiskAssessmentCompletedEvent event = eventCaptor.getValue();
        assertThat(event.getRiskCheckRequestId()).isEqualTo(riskCheckRequest.getId().toString());
        assertThat(event.getPaymentId()).isEqualTo(riskCheckRequest.getPaymentId());
        assertThat(event.getRiskScore()).isEqualTo(riskAssessment.getRiskScore().doubleValue());
        // Current behavior: the fraud event's response timestamp is passed through as processingTimeMs
        assertThat(event.getProcessingTimeMs()).isEqualTo(fraudAnalysisCompletedEvent.getTimestamp());
    }
}
