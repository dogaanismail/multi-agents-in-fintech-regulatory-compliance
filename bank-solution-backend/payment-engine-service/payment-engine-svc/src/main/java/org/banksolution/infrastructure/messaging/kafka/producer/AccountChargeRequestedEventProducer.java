package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.account.AccountChargeRequestedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.event.AccountChargeInitiatedEvent;
import org.banksolution.infrastructure.messaging.kafka.mapper.AccountChargeRequestedEventMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountChargeRequestedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull AccountChargeRequestedEvent> accountChargeRequestedEventKafkaTemplate;

    public void publishAccountChargeRequestedEvent(AccountChargeInitiatedEvent event) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getAccountChargeRequested();
            String messageKey = event.paymentId().toString();

            AccountChargeRequestedEvent request = AccountChargeRequestedEventMapper.toAvroRequest(event);
            accountChargeRequestedEventKafkaTemplate.send(topic, messageKey, request);
            log.info("Successfully published AccountChargeRequestedEvent for payment: {}", event.paymentId());
        } catch (Exception e) {
            log.error("Error publishing AccountChargeRequestedEvent for payment: {}", event.paymentId(), e);
            throw e;
        }
    }
}
