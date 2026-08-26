package org.banksolution.producer;

import com.aml.payment.PaymentCreatedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.entity.PaymentRequestEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.banksolution.mapper.PaymentCreatedEventMapper.toPaymentCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull PaymentCreatedEvent> paymentCreatedEventKafkaTemplate;

    public void publishPaymentCreatedEvent(
            PaymentRequestEntity paymentRequestEntity,
            boolean isCrossBorderPayment) {

        log.info("Publishing PaymentCreatedEvent for payment: {}", paymentRequestEntity.getId());

        String paymentCreatedTopic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentCreated();
        String messageKey = paymentRequestEntity.getId().toString();
        PaymentCreatedEvent paymentCreatedEvent = toPaymentCreatedEvent(paymentRequestEntity, isCrossBorderPayment);
        paymentCreatedEventKafkaTemplate.send(paymentCreatedTopic, messageKey, paymentCreatedEvent);

        log.info("Published PaymentCreatedEvent: eventId:{}, paymentId:{}, type:{}",
                paymentCreatedEvent.getEventId(),
                paymentCreatedEvent.getPaymentId(),
                paymentCreatedEvent.getPaymentType());
    }
}

