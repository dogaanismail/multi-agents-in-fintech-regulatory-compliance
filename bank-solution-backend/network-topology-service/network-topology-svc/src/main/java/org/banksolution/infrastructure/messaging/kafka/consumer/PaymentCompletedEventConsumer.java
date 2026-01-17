package org.banksolution.infrastructure.messaging.kafka.consumer;

import com.aml.payment.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.service.TransactionGraphService;
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

    private final TransactionGraphService transactionGraphService;

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

        log.info("Received PaymentCompletedEvent: eventId: {}, paymentId: {}, partition: {}, offset: {}",
                event.getEventId(),
                event.getPaymentId(),
                partition,
                offset);

        try {
            if (isValidPayment(event)) {
                transactionGraphService.createTransactionRelationship(event);
            } else {
                log.debug("Skipping event: riskCheckPassed: {}, sourceAccount: {}, destAccount: {}",
                        event.getRiskCheckPassed(),
                        event.getSourceAccountId(),
                        event.getDestinationAccountId());
            }

            acknowledgment.acknowledge();
            log.info("Successfully processed PaymentCompletedEvent for paymentId: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to process PaymentCompletedEvent for paymentId: {}", event.getPaymentId(), e);
            throw e;
        }
    }

    private boolean isValidPayment(PaymentCompletedEvent event) {
        return event.getSourceAccountId() != null && event.getDestinationAccountId() != null;
    }
}