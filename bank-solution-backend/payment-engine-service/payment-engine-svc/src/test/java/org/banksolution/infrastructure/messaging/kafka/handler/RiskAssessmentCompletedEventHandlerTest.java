package org.banksolution.infrastructure.messaging.kafka.handler;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.banksolution.domain.payment.event.RiskAssessmentCompletedEvent;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.fixtures.PaymentFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RiskAssessmentCompletedEventHandlerTest {

    private EventGateway eventGateway;
    private RiskAssessmentCompletedEventHandler handler;

    @BeforeEach
    void setUp() {
        eventGateway = mock(EventGateway.class);
        handler = new RiskAssessmentCompletedEventHandler(eventGateway);
    }

    @Test
    void shouldPublishDomainRiskAssessmentCompletedEvent() {
        handler.handle(AvroEventFixtures.riskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventGateway).publish(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(RiskAssessmentCompletedEvent.class);
        RiskAssessmentCompletedEvent published = (RiskAssessmentCompletedEvent) captor.getValue();
        assertThat(published.paymentId().getIdentifier()).isEqualTo(PaymentFixtures.PAYMENT_UUID);
        assertThat(published.riskAssessment().riskAction()).isEqualTo("PROCEED");
        assertThat(published.riskAssessment().riskLevel()).isEqualTo("LOW");
        assertThat(published.riskAssessment().riskScore()).isEqualTo(0.10);
    }
}
