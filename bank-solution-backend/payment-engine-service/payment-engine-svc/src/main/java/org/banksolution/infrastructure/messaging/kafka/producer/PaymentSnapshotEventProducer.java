package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.banksolution.infrastructure.messaging.kafka.mapper.PaymentAggregateSnapshotMapper.toSnapshot;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotEventKafkaTemplate;

    public void publishPaymentSnapshot(PaymentAggregate aggregate, String eventTrigger) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentSnapshotEvents();
            String messageKey = aggregate.getPaymentId().toString();

            PaymentSnapshotEvent snapshot = toSnapshot(aggregate, eventTrigger);
            paymentSnapshotEventKafkaTemplate.send(topic, messageKey, snapshot);

            log.info("Successfully published PaymentSnapshotEvent for payment: {}, trigger: {}, version: {}",
                    aggregate.getPaymentId(), eventTrigger, aggregate.getVersion());
        } catch (Exception e) {
            log.error("Error publishing PaymentSnapshotEvent for payment: {}", aggregate.getPaymentId(), e);
            // Don't throw - snapshot publishing shouldn't break the main flow
        }
    }
}
