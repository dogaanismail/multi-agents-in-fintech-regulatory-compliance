package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentBlockedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentBlockedEventProducer {

    private final KafkaTemplate<@NonNull String, @NonNull PaymentBlockedEvent> paymentBlockedEventKafkaTemplate;

    @Value("${kafka.topics.payment-blocked}")
    private String paymentBlockedTopic;

    public void publishPaymentBlocked(org.banksolution.domain.payment.event.PaymentBlockedEvent event) {
        log.info("Publishing PaymentBlockedEvent for payment: {}", event.getPaymentId());

        PaymentBlockedEvent blockedEvent = PaymentBlockedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(event.getPaymentId().asUUID().toString())
                .setBlockReason(event.getReason())
                .setFraudScore(event.getConfidence())
                .setTimestamp(Instant.now().getEpochSecond())
                .build();

        paymentBlockedEventKafkaTemplate.send(
                paymentBlockedTopic,
                event.getPaymentId().toString(),
                blockedEvent
        );

        log.info("Published PaymentBlockedEvent: eventId:{}, paymentId:{}, reason:{}",
                blockedEvent.getEventId(),
                blockedEvent.getPaymentId(),
                blockedEvent.getBlockReason());
    }
}
