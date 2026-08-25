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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSnapshotEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotEventKafkaTemplate;
    private final PaymentQueryService paymentQueryService;

    public void publish(PaymentId paymentId, PaymentEventTrigger paymentEventTrigger) {
        try {
            log.debug("Publishing payment snapshot for paymentId: {}, trigger: {}", paymentId, paymentEventTrigger);

            PaymentResponse paymentResponse = paymentQueryService.findPaymentById(paymentId);
            PaymentSnapshotEvent paymentSnapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(paymentResponse, paymentEventTrigger.name());

            String paymentSnapshotTopic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentSnapshotEvents();
            String messageKey = paymentSnapshotEvent.getPaymentId();

            paymentSnapshotEventKafkaTemplate.send(paymentSnapshotTopic, messageKey, paymentSnapshotEvent);

            log.info("Successfully published PaymentSnapshotEvent for paymentId: {}, status: {}, trigger: {}",
                    paymentId,
                    paymentResponse.status(),
                    paymentEventTrigger);
        } catch (Exception exception) {
            log.error("Failed to publish payment snapshot for paymentId: {}, trigger: {}",
                    paymentId,
                    paymentEventTrigger,
                    exception);
        }
    }
}

