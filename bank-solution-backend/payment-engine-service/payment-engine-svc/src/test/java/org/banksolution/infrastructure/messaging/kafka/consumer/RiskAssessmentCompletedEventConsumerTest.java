package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentCompletedEvent;
import com.aml.risk.RiskLevel;
import org.banksolution.exception.RiskAssessmentCompletedEventException;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.infrastructure.messaging.kafka.handler.RiskAssessmentCompletedEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RiskAssessmentCompletedEventConsumerTest {

    private RiskAssessmentCompletedEventHandler handler;
    private Acknowledgment acknowledgment;
    private RiskAssessmentCompletedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        handler = mock(RiskAssessmentCompletedEventHandler.class);
        acknowledgment = mock(Acknowledgment.class);
        consumer = new RiskAssessmentCompletedEventConsumer(handler);
    }

    @Test
    void shouldDelegateToHandlerAndAcknowledge() {
        RiskAssessmentCompletedEvent event = AvroEventFixtures.riskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10);

        consumer.consume(event, 0, 0L, acknowledgment);

        verify(handler).handle(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldWrapHandlerFailureAndNotAcknowledge() {
        RiskAssessmentCompletedEvent event = AvroEventFixtures.riskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10);
        doThrow(new RuntimeException("boom")).when(handler).handle(any());

        assertThatThrownBy(() -> consumer.consume(event, 0, 0L, acknowledgment))
                .isInstanceOf(RiskAssessmentCompletedEventException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
