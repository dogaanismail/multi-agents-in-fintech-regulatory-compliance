package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.ledger.LedgerPostingRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.LedgerPostingRequestService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingRequestedEventConsumer {

    private final LedgerPostingRequestService ledgerPostingRequestService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.ledger-posting-requested}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload LedgerPostingRequestedEvent ledgerPostingRequestedEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed LedgerPostingRequestedEvent: clientTransactionId:{}, type:{}, partition:{}, offset:{}",
                ledgerPostingRequestedEvent.getClientTransactionId(),
                ledgerPostingRequestedEvent.getPostingInstructionType(),
                partition,
                offset);

        ledgerPostingRequestService.processLedgerPostingRequest(ledgerPostingRequestedEvent);

        acknowledgment.acknowledge();
    }
}
