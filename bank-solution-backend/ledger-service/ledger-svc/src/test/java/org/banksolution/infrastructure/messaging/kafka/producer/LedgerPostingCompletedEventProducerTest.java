package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.PostingInstructionType;
import org.banksolution.config.KafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LedgerPostingCompletedEventProducerTest {

    private static final String TOPIC = "ledger.posting.completed";

    private KafkaTemplate<String, LedgerPostingCompletedEvent> ledgerPostingCompletedEventKafkaTemplate;
    private LedgerPostingCompletedEventProducer ledgerPostingCompletedEventProducer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setLedgerPostingCompleted(TOPIC);
        ledgerPostingCompletedEventKafkaTemplate = mock(KafkaTemplate.class);
        ledgerPostingCompletedEventProducer =
                new LedgerPostingCompletedEventProducer(kafkaConfigurationProperties, ledgerPostingCompletedEventKafkaTemplate);
    }

    @Test
    void shouldPublishTheOutcomeKeyedByClientTransactionIdSoOnePaymentStaysOrdered() {
        String clientTransactionId = UUID.randomUUID().toString();
        LedgerPostingCompletedEvent ledgerPostingCompletedEvent = LedgerPostingCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setClientTransactionId(clientTransactionId)
                .setPostingInstructionType(PostingInstructionType.SETTLEMENT)
                .setSuccess(true)
                .setTimestamp(1L)
                .build();

        ledgerPostingCompletedEventProducer.publish(ledgerPostingCompletedEvent);

        verify(ledgerPostingCompletedEventKafkaTemplate).send(TOPIC, clientTransactionId, ledgerPostingCompletedEvent);
    }
}
