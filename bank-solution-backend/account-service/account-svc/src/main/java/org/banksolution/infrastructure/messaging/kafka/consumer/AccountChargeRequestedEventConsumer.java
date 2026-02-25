package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.account.AccountChargeRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.infrastructure.messaging.kafka.handler.AccountChargeHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountChargeRequestedEventConsumer {

    private final AccountChargeHandler accountChargeHandler;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.account-charge-requested}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload AccountChargeRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received AccountChargeRequestedEvent: paymentId:{}, paymentType:{}, partition:{}, offset:{}",
                event.getPaymentId(),
                event.getPaymentType(),
                partition,
                offset);

        try {
            accountChargeHandler.handle(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process AccountChargeRequestedEvent: paymentId:{}", event.getPaymentId(), e);
            throw e;
        }
    }
}
