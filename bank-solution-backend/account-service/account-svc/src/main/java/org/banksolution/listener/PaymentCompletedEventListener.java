package org.banksolution.listener;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.handler.AccountBalanceHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventListener {

    private final AccountBalanceHandler accountBalanceHandler;

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

        log.info("Received PaymentCompletedEvent: eventId:{}, paymentId:{}, partition:{}, offset:{}",
                event.getEventId(),
                event.getPaymentId(),
                partition,
                offset);

        try {
            if (event.getRiskCheckPassed()) {
                accountBalanceHandler.processPaymentCompletedEvent(event);
            }

            acknowledgment.acknowledge();
            log.info("Successfully processed payment event: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", event.getEventId(), e);
            throw e;
        }
    }
}
