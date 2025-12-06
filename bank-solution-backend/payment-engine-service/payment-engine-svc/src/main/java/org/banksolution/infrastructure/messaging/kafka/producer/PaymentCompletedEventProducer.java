package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentCompletedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventProducer {

    private final KafkaTemplate<@NonNull String, @NonNull PaymentCompletedEvent> paymentCompletedEventKafkaTemplate;

    @Value("${kafka.topics.payment-completed}")
    private String paymentCompletedTopic;

    public void publishPaymentCompleted(PaymentId paymentId) {
        log.info("Publishing PaymentCompletedEvent for payment: {}", paymentId);

        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(paymentId.asUUID().toString())
                .setRiskCheckPassed(true)
                .setRiskScore(0.0) // Risk score already stored in RiskCheckCompletedEvent
                .setTimestamp(Instant.now().getEpochSecond())
                .build();

        paymentCompletedEventKafkaTemplate.send(
                paymentCompletedTopic,
                paymentId.asUUID().toString(),
                completedEvent
        );

        log.info("Published PaymentCompletedEvent: eventId:{}, paymentId:{}",
                completedEvent.getEventId(),
                completedEvent.getPaymentId());
    }
}
