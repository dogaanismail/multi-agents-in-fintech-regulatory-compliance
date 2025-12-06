package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotProducer {

    private final KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotKafkaTemplate;

    @Value("${kafka.topics.payment-snapshot-events}")
    private String paymentSnapshotTopic;

    @EventHandler
    public void handle(PaymentSnapshotEvent snapshot) {
        String key = snapshot.getReferenceNumber();

        log.info("Publishing payment snapshot to Kafka: referenceNumber:{}, version:{}, trigger:{}",
                key,
                snapshot.getVersion(),
                snapshot.getEventTrigger());

        paymentSnapshotKafkaTemplate.send(paymentSnapshotTopic, key, snapshot);
        log.info("Payment snapshot published successfully to Kafka topic: {}", paymentSnapshotTopic);
    }
}
