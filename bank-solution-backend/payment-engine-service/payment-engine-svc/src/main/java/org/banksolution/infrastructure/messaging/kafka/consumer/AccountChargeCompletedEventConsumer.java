package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.account.AccountChargeCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.exception.AccountChargeCompletedEventException;
import org.banksolution.infrastructure.messaging.kafka.handler.AccountChargeCompletedEventHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountChargeCompletedEventConsumer {

    private final AccountChargeCompletedEventHandler accountChargeCompletedEventHandler;

    @KafkaListener(
            topics = "${spring.kafka.topics.incoming.account-charge-completed}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload AccountChargeCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Consumed AccountChargeCompletedEvent: paymentId:{}, success:{}, partition:{}, offset:{}",
                event.getPaymentId(),
                event.getSuccess(),
                partition,
                offset);

        try {
            accountChargeCompletedEventHandler.handle(event);
            acknowledgment.acknowledge();
            log.info("Successfully processed AccountChargeCompletedEvent for paymentId:{}",
                    event.getPaymentId());
        } catch (Exception e) {
            log.error("Failed to process AccountChargeCompletedEvent for paymentId:{}",
                    event.getPaymentId(),
                    e);
            throw new AccountChargeCompletedEventException("Failed to process AccountChargeCompletedEvent for paymentId: %s",
                    e,
                    event.getPaymentId());
        }
    }
}
