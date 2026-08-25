package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.exception.RiskAssessmentRequestedEventException;
import org.banksolution.service.RiskAssessmentRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createRiskAssessmentRequestedEvent;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentRequestedEventConsumerTest {

    private static final String PAYMENT_ID = "PAY-1";
    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private RiskAssessmentRequestService riskAssessmentRequestService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private RiskAssessmentRequestedEventConsumer riskAssessmentRequestedEventConsumer;

    @Test
    void shouldAcknowledgeAfterProcessingTheEvent() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);

        riskAssessmentRequestedEventConsumer.consume(event, PARTITION, OFFSET, acknowledgment);

        verify(riskAssessmentRequestService).processRiskAssessmentRequest(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenProcessingFails() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);
        doThrow(new IllegalStateException("database down"))
                .when(riskAssessmentRequestService).processRiskAssessmentRequest(event);

        assertThatThrownBy(() ->
                riskAssessmentRequestedEventConsumer.consume(event, PARTITION, OFFSET, acknowledgment))
                .isInstanceOf(RiskAssessmentRequestedEventException.class)
                .hasMessageContaining(PAYMENT_ID);

        verify(acknowledgment, never()).acknowledge();
    }
}
