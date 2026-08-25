package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentCompletedEvent;
import com.aml.risk.RiskLevel;
import org.banksolution.exception.RiskAssessmentCompletedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.RiskAssessmentCompletedEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.PAYMENT_UUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentCompletedEventConsumerTest {

    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private RiskAssessmentCompletedEventHandler riskAssessmentCompletedEventHandler;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private RiskAssessmentCompletedEventConsumer riskAssessmentCompletedEventConsumer;

    @Test
    void shouldAcknowledgeAfterPublishingTheAssessment() {
        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent =
                createRiskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10);

        riskAssessmentCompletedEventConsumer.consume(riskAssessmentCompletedEvent, PARTITION, OFFSET, acknowledgment);

        verify(riskAssessmentCompletedEventHandler).handle(riskAssessmentCompletedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenPublishingFails() {
        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent =
                createRiskAssessmentCompletedEvent(RiskAction.PROCEED, RiskLevel.LOW, 0.10);
        IllegalStateException publishingFailure = new IllegalStateException("event bus down");
        doThrow(publishingFailure).when(riskAssessmentCompletedEventHandler).handle(riskAssessmentCompletedEvent);

        assertThatThrownBy(() -> riskAssessmentCompletedEventConsumer.consume(
                riskAssessmentCompletedEvent, PARTITION, OFFSET, acknowledgment))
                .isInstanceOf(RiskAssessmentCompletedEventException.class)
                .hasMessageContaining(PAYMENT_UUID.toString())
                .hasMessageContaining(riskAssessmentCompletedEvent.getRiskCheckRequestId())
                .hasCause(publishingFailure);

        verify(acknowledgment, never()).acknowledge();
    }
}
