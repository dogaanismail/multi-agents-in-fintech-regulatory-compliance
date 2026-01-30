package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.PaymentEventProcessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventConsumer {

    private final PaymentEventProcessor paymentEventProcessor;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentCompletedEvent(
            @Payload PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received PaymentCompletedEvent: paymentId: {}, partition: {}, offset: {}",
                event.getPaymentId(),
                partition,
                offset);

        try {
            paymentEventProcessor.process(event);
            acknowledgment.acknowledge();
            log.debug("Acknowledged PaymentCompletedEvent: paymentId: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent: paymentId: {}", event.getPaymentId(), e);
            throw e;
        }
    }
}
