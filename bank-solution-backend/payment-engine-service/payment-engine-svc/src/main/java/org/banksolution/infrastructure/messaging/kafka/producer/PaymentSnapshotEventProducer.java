package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentEventTrigger;
import org.banksolution.infrastructure.messaging.kafka.mapper.PaymentAggregateSnapshotMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotEventKafkaTemplate;
    private final PaymentQueryService paymentQueryService;

    @Async
    public void publish(PaymentId paymentId, PaymentEventTrigger eventTrigger) {
        try {
            log.debug("Publishing payment snapshot for paymentId: {}, trigger: {}", paymentId, eventTrigger);

            PaymentResponse paymentResponse = paymentQueryService.findPaymentById(paymentId);
            PaymentSnapshotEvent snapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, eventTrigger.name());

            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentSnapshotEvents();
            String messageKey = snapshotEvent.getPaymentId();

            paymentSnapshotEventKafkaTemplate.send(topic, messageKey, snapshotEvent);

            log.info("Successfully published PaymentSnapshotEvent for paymentId: {}, status: {}, trigger: {}",
                    paymentId,
                    paymentResponse.status(),
                    eventTrigger);
        } catch (Exception e) {
            log.error("Failed to publish payment snapshot for paymentId: {}, trigger: {}",
                    paymentId,
                    eventTrigger,
                    e);
        }
    }
}

