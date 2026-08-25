package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import org.banksolution.exception.FraudAnalysisCompletedEventException;
import org.banksolution.service.FraudAnalysisCompleteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FraudAnalysisCompletedEventConsumerTest {

    private static final String PAYMENT_ID = "PAY-1";
    private static final int PARTITION = 0;
    private static final long OFFSET = 42L;

    @Mock
    private FraudAnalysisCompleteService fraudAnalysisCompleteService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private FraudAnalysisCompletedEventConsumer fraudAnalysisCompletedEventConsumer;

    @Test
    void shouldAcknowledgeAfterProcessingTheEvent() {
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(UUID.randomUUID().toString(), PAYMENT_ID);

        fraudAnalysisCompletedEventConsumer.consume(event, PARTITION, OFFSET, acknowledgment);

        verify(fraudAnalysisCompleteService).processFraudAnalysisCompleted(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldRethrowWithoutAcknowledgingWhenProcessingFails() {
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(UUID.randomUUID().toString(), PAYMENT_ID);
        doThrow(new IllegalStateException("database down"))
                .when(fraudAnalysisCompleteService).processFraudAnalysisCompleted(event);

        assertThatThrownBy(() ->
                fraudAnalysisCompletedEventConsumer.consume(event, PARTITION, OFFSET, acknowledgment))
                .isInstanceOf(FraudAnalysisCompletedEventException.class)
                .hasMessageContaining(PAYMENT_ID)
                .hasMessageContaining(event.getRiskCheckRequestId());

        verify(acknowledgment, never()).acknowledge();
    }
}
