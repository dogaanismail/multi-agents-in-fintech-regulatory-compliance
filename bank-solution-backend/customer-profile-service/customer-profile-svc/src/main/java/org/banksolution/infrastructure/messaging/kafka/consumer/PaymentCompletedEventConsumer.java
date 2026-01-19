package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.ProfileAggregationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventConsumer {

    private final ProfileAggregationService profileAggregationService;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompletedEvent(
            @Payload PaymentCompletedEvent event,
            Acknowledgment acknowledgment) {

        log.info("Received PaymentCompletedEvent: paymentId: {}", event.getPaymentId());

        try {
            UUID customerId = UUID.fromString(event.getCustomerId());
            profileAggregationService.processPaymentEvent(event);
            profileAggregationService.updateCustomerProfile(customerId);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent: paymentId: {}", event.getPaymentId(), e);
            throw e;
        }
    }
}
