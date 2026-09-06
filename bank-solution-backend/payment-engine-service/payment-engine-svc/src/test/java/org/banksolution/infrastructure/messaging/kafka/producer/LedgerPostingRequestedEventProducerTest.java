package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import org.banksolution.config.KafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.banksolution.exception.KafkaPublicationException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(ledgerPostingRequestedEventKafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
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

    @Test
    void shouldFailTheSagaWhenTheBrokerNeverAcknowledgesThePosting() {
        IllegalStateException brokerFailure = new IllegalStateException("broker unavailable");
        when(ledgerPostingRequestedEventKafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(brokerFailure));

        assertThatThrownBy(() -> ledgerPostingRequestedEventProducer.publishSettlement(createPaymentId()))
                .isInstanceOf(KafkaPublicationException.class)
                .hasMessageContaining(TOPIC)
                .hasMessageContaining(PAYMENT_UUID.toString())
                .hasCause(brokerFailure);
    }
}
