package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotEventKafkaTemplate;

    public void publishPaymentSnapshot(PaymentSnapshotEvent snapshot) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentSnapshotEvents();
            String messageKey = snapshot.getPaymentId();

            paymentSnapshotEventKafkaTemplate.send(topic, messageKey, snapshot);
            log.info("Successfully published PaymentSnapshotEvent for payment: {}, status: {}, trigger: {}",
                    snapshot.getPaymentId(),
                    snapshot.getStatus(),
                    snapshot.getEventTrigger());
        } catch (Exception e) {
            log.error("Error publishing PaymentSnapshotEvent for payment: {}, trigger: {}",
                    snapshot.getPaymentId(),
                    snapshot.getEventTrigger(),
                    e);
        }
    }
}

