package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.banksolution.domain.payment.event.RiskAssessmentCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEventWithMarl;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentId;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentCompletedEventHandlerTest {

    @Mock
    private EventGateway eventGateway;

    @InjectMocks
    private RiskAssessmentCompletedEventHandler riskAssessmentCompletedEventHandler;

    @Test
    void shouldPublishTheDomainEventForTheSagaToPickUp() {
        riskAssessmentCompletedEventHandler.handle(
                createRiskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10));

        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent = capturePublishedEvent();
        assertThat(riskAssessmentCompletedEvent.paymentId()).isEqualTo(createPaymentId());
        assertThat(riskAssessmentCompletedEvent.riskAssessment().riskAction()).isEqualTo("PROCEED");
        assertThat(riskAssessmentCompletedEvent.riskAssessment().riskLevel()).isEqualTo("LOW");
        assertThat(riskAssessmentCompletedEvent.riskAssessment().riskScore()).isEqualTo(0.10);
        assertThat(riskAssessmentCompletedEvent.riskAssessment().marlAssessment()).isNull();
    }

    @Test
    void shouldCarryTheMarlAssessmentWhenTheOrchestratorContributed() {
        riskAssessmentCompletedEventHandler.handle(createRiskAssessmentCompletedEventWithMarl(RiskAction.BLOCK));

        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent = capturePublishedEvent();
        assertThat(riskAssessmentCompletedEvent.riskAssessment().marlAssessment().action()).isEqualTo("BLOCK");
        assertThat(riskAssessmentCompletedEvent.riskAssessment().marlAssessment().maddpgQValue()).isEqualTo(0.42);
    }

    private RiskAssessmentCompletedEvent capturePublishedEvent() {
        ArgumentCaptor<Object> publishedEventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventGateway).publish(publishedEventCaptor.capture());

        assertThat(publishedEventCaptor.getValue()).isInstanceOf(RiskAssessmentCompletedEvent.class);

        return (RiskAssessmentCompletedEvent) publishedEventCaptor.getValue();
    }
}
