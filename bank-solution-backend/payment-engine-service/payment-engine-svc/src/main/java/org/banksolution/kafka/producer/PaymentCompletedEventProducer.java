package org.banksolution.kafka.producer;

import com.aml.payment.PaymentCompletedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.event.FraudCheckApprovedEvent;
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

    public void publishPaymentCompleted(FraudCheckApprovedEvent event) {
        log.info("Publishing PaymentCompletedEvent for payment: {}", event.getPaymentId());

        PaymentCompletedEvent completedEvent = PaymentCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(event.getPaymentId().asUUID().toString())
                .setRiskCheckPassed(true)
                .setRiskScore(event.getConfidence())
                .setTimestamp(Instant.now().getEpochSecond())
                .build();

        paymentCompletedEventKafkaTemplate.send(
                paymentCompletedTopic,
                event.getPaymentId().toString(),
                completedEvent
        );

        log.info("Published PaymentCompletedEvent: eventId:{}, paymentId:{}",
                completedEvent.getEventId(),
                completedEvent.getPaymentId());
    }
}
