package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.payment.PaymentCompletedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.infrastructure.messaging.kafka.mapper.PaymentCompletedEventMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull PaymentCompletedEvent> paymentCompletedEventKafkaTemplate;
    private final PaymentQueryService paymentQueryService;

    @Async
    public void publish(PaymentId paymentId) {
        try {
            log.debug("Publishing payment completed event for paymentId: {}", paymentId);

            PaymentResponse paymentResponse = paymentQueryService.findPaymentById(paymentId);
            PaymentCompletedEvent completedEvent = PaymentCompletedEventMapper.toAvroEvent(paymentResponse);

            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getPaymentCompleted();
            String messageKey = completedEvent.getPaymentId();

            paymentCompletedEventKafkaTemplate.send(topic, messageKey, completedEvent);

            log.info("Successfully published PaymentCompletedEvent: paymentId={}, customerId={}, amount={}, currency={}",
                    completedEvent.getPaymentId(),
                    completedEvent.getCustomerId(),
                    completedEvent.getAmount(),
                    completedEvent.getFromCurrency());
        } catch (Exception e) {
            log.error("Failed to publish payment completed event for paymentId: {}", paymentId, e);
        }
    }
}
