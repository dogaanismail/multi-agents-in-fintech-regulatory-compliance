package org.banksolution.producer;

import com.aml.payment.PaymentCreatedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentRequestEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.banksolution.mapper.PaymentCreatedEventMapper.toPaymentCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<@NonNull String, @NonNull PaymentCreatedEvent> paymentCreatedEventKafkaTemplate;

    @Value("${kafka.topics.payment-created}")
    private String paymentCreatedTopic;

    public void publishPaymentCreatedEvent(PaymentRequestEntity paymentRequest) {
        log.info("Publishing PaymentCreatedEvent for payment: {}", paymentRequest.getId());

        PaymentCreatedEvent event = toPaymentCreatedEvent(paymentRequest);

        paymentCreatedEventKafkaTemplate.send(paymentCreatedTopic, paymentRequest.getId().toString(), event);

        log.info("Published PaymentCreatedEvent: eventId:{}, paymentId:{}, referenceNumber:{}, type:{}",
                event.getEventId(),
                event.getPaymentId(),
                event.getReferenceNumber(),
                event.getPaymentType());
    }
}

