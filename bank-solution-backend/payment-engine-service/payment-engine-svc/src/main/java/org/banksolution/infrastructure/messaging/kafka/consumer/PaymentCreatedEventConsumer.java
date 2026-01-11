package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.payment.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.PaymentCreatedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.PaymentCreatedEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventConsumer {

    private final PaymentCreatedEventHandler paymentCreatedEventHandler;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.payment-created}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload PaymentCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed PaymentCreatedEvent: eventId:{}, paymentId:{}, partition:{}, offset:{}",
                event.getEventId(),
                event.getPaymentId(),
                partition,
                offset);

        try {
            paymentCreatedEventHandler.handle(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed PaymentCreatedEvent: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process PaymentCreatedEvent: {}", event.getEventId(), e);
            throw new PaymentCreatedEventException("Failed to process PaymentCreatedEvent for paymentId: %s", e, event.getPaymentId());
        }
    }
}
