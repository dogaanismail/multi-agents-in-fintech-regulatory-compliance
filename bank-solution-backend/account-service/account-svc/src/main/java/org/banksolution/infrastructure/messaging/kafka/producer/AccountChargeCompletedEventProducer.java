package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.account.AccountChargeCompletedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountChargeCompletedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull AccountChargeCompletedEvent> accountChargeCompletedEventKafkaTemplate;

    public void publish(AccountChargeCompletedEvent event) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getAccountChargeCompleted();
            String messageKey = event.getPaymentId();

            accountChargeCompletedEventKafkaTemplate.send(topic, messageKey, event);
            log.info("Published AccountChargeCompletedEvent: paymentId:{}, success:{}",
                    event.getPaymentId(), event.getSuccess());
        } catch (Exception e) {
            log.error("Failed to publish AccountChargeCompletedEvent for paymentId:{}", event.getPaymentId(), e);
            throw e;
        }
    }
}
