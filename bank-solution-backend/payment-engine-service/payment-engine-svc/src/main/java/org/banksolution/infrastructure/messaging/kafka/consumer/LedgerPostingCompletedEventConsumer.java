package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.ledger.LedgerPostingCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.LedgerPostingCompletedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.LedgerPostingCompletedEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingCompletedEventConsumer {

    private final LedgerPostingCompletedEventHandler ledgerPostingCompletedEventHandler;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.ledger-posting-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload LedgerPostingCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed LedgerPostingCompletedEvent: clientTransactionId:{}, type:{}, success:{}, partition:{}, offset:{}",
                event.getClientTransactionId(),
                event.getPostingInstructionType(),
                event.getSuccess(),
                partition,
                offset);

        try {
            ledgerPostingCompletedEventHandler.handle(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            throw new LedgerPostingCompletedEventException(
                    "Failed to process LedgerPostingCompletedEvent for clientTransactionId: %s",
                    e,
                    event.getClientTransactionId());
        }
    }
}
