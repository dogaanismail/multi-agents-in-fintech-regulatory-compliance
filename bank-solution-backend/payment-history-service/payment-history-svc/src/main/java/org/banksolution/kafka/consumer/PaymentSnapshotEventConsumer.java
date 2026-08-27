package org.banksolution.kafka.consumer;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.PaymentHistoryAggregationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotEventConsumer {

    private final PaymentHistoryAggregationService paymentHistoryAggregationService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.payment-snapshot-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentSnapshotKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload PaymentSnapshotEvent paymentSnapshotEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info("Consumed PaymentSnapshotEvent from topic: {}, partition: {}, offset: {}, " +
                        "paymentId: {}, referenceNumber: {}, version: {}, trigger: {}",
                topic,
                partition,
                offset,
                paymentSnapshotEvent.getPaymentId(),
                paymentSnapshotEvent.getReferenceNumber(),
                paymentSnapshotEvent.getVersion(),
                paymentSnapshotEvent.getEventTrigger());

        try {
            paymentHistoryAggregationService.processPaymentSnapshotEvent(paymentSnapshotEvent);
            acknowledgment.acknowledge();
            log.info("Successfully processed PaymentSnapshotEvent for paymentId: {}, referenceNumber: {}, version: {}",
                    paymentSnapshotEvent.getPaymentId(),
                    paymentSnapshotEvent.getReferenceNumber(),
                    paymentSnapshotEvent.getVersion());
        } catch (Exception processingFailure) {
            log.error("Error processing PaymentSnapshotEvent for paymentId: {}, referenceNumber: {}",
                    paymentSnapshotEvent.getPaymentId(),
                    paymentSnapshotEvent.getReferenceNumber(),
                    processingFailure);
            throw processingFailure;
        }
    }
}
