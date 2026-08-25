package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import org.banksolution.config.KafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LedgerPostingRequestedEventProducerTest {

    private static final String TOPIC = "ledger.posting.requested";

    private KafkaTemplate<String, LedgerPostingRequestedEvent> ledgerPostingRequestedEventKafkaTemplate;
    private LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setLedgerPostingRequested(TOPIC);
        ledgerPostingRequestedEventKafkaTemplate = mock(KafkaTemplate.class);
        ledgerPostingRequestedEventProducer =
                new LedgerPostingRequestedEventProducer(kafkaConfigurationProperties, ledgerPostingRequestedEventKafkaTemplate);
    }

    @Test
    void shouldPublishEveryPostingKeyedByTheClientTransactionId() {
        ledgerPostingRequestedEventProducer.publishAuthorisation(createLedgerAuthorisationInitiatedEvent());
        ledgerPostingRequestedEventProducer.publishSettlement(createPaymentId());
        ledgerPostingRequestedEventProducer.publishRelease(createPaymentId());

        ArgumentCaptor<LedgerPostingRequestedEvent> ledgerPostingRequestedEventCaptor =
                ArgumentCaptor.forClass(LedgerPostingRequestedEvent.class);
        verify(ledgerPostingRequestedEventKafkaTemplate, times(3))
                .send(eq(TOPIC), eq(PAYMENT_UUID.toString()), ledgerPostingRequestedEventCaptor.capture());
        assertThat(ledgerPostingRequestedEventCaptor.getAllValues())
                .extracting(LedgerPostingRequestedEvent::getPostingInstructionType)
                .containsExactly(
                        PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION,
                        PostingInstructionType.SETTLEMENT,
                        PostingInstructionType.RELEASE);
    }
}
