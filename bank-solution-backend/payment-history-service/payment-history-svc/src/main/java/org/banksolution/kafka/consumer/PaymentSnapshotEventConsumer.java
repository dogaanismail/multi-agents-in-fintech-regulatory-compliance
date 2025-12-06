package org.banksolution.kafka.consumer;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.PaymentHistoryAggregationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotEventConsumer {

    private final PaymentHistoryAggregationService historyService;

    @KafkaListener(
            topics = "${kafka.topics.payment-snapshot-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentSnapshotKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload PaymentSnapshotEvent snapshot,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.info("Consumed PaymentSnapshotEvent from topic: {}, partition: {}, offset: {}, " + "referenceNumber: {}, version: {}, trigger: {}",
                topic,
                partition,
                offset,
                snapshot.getReferenceNumber(),
                snapshot.getVersion(),
                snapshot.getEventTrigger());

        try {
            historyService.processPaymentSnapshot(snapshot);
            log.info("Successfully processed PaymentSnapshotEvent for referenceNumber: {}, version: {}",
                    snapshot.getReferenceNumber(),
                    snapshot.getVersion());
        } catch (Exception e) {
            log.error("Error processing PaymentSnapshotEvent for referenceNumber: {}",
                    snapshot.getReferenceNumber(),
                    e);
            throw e;
        }
    }
}
