package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.ledger.LedgerPostingRequestedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.event.LedgerAuthorisationInitiatedEvent;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.mapper.LedgerPostingRequestedEventMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingRequestedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull LedgerPostingRequestedEvent> ledgerPostingRequestedEventKafkaTemplate;

    public void publishAuthorisation(LedgerAuthorisationInitiatedEvent ledgerAuthorisationInitiatedEvent) {
        publish(LedgerPostingRequestedEventMapper.toAuthorisationEvent(ledgerAuthorisationInitiatedEvent));
    }

    public void publishSettlement(PaymentId paymentId) {
        publish(LedgerPostingRequestedEventMapper.toSettlementEvent(paymentId));
    }

    public void publishRelease(PaymentId paymentId) {
        publish(LedgerPostingRequestedEventMapper.toReleaseEvent(paymentId));
    }

    private void publish(LedgerPostingRequestedEvent ledgerPostingRequestedEvent) {
        String ledgerPostingRequestedTopic = kafkaConfigurationProperties.getTopics().getOutgoing().getLedgerPostingRequested();

        String messageKey = ledgerPostingRequestedEvent.getClientTransactionId();

        KafkaDeliveryAwaiter.awaitDelivery(
                ledgerPostingRequestedEventKafkaTemplate.send(ledgerPostingRequestedTopic, messageKey, ledgerPostingRequestedEvent),
                ledgerPostingRequestedTopic,
                messageKey);

        log.info("Published LedgerPostingRequestedEvent: clientTransactionId:{}, type:{}",
                ledgerPostingRequestedEvent.getClientTransactionId(),
                ledgerPostingRequestedEvent.getPostingInstructionType());
    }
}
