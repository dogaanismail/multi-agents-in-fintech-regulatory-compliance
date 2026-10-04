package org.banksolution.producer;

import com.aml.payment.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.springframework.stereotype.Component;

import static org.banksolution.mapper.PaymentCreatedEventMapper.toPaymentCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventProducer {

    private static final String PAYMENT_CREATED_IDEMPOTENCE_KEY_PREFIX = "payment-created:";

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final OutboxEventPublisher outboxEventPublisher;

    public void publishPaymentCreatedEvent(
            PaymentRequestEntity paymentRequestEntity,
            boolean isCrossBorderPayment) {

        String paymentCreatedTopic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentCreated();
        String paymentId = paymentRequestEntity.getId().toString();
        PaymentCreatedEvent paymentCreatedEvent = toPaymentCreatedEvent(paymentRequestEntity, isCrossBorderPayment);

        outboxEventPublisher.publish(
                paymentCreatedTopic,
                paymentId,
                paymentCreatedEvent,
                PAYMENT_CREATED_IDEMPOTENCE_KEY_PREFIX + paymentId);

        log.info("Stored PaymentCreatedEvent in outbox: eventId:{}, paymentId:{}, type:{}",
                paymentCreatedEvent.getEventId(),
                paymentCreatedEvent.getPaymentId(),
                paymentCreatedEvent.getPaymentType());
    }
}
