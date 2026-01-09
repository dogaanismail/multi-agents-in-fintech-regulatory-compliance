package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskCheckRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.event.RiskCheckRequestedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.banksolution.infrastructure.messaging.kafka.mapper.RiskCheckRequestMapper.toAvroRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckRequestProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull RiskCheckRequest> riskCheckKafkaTemplate;

    public void publishRiskCheckRequestedEvent(RiskCheckRequestedEvent event) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getRiskCheckRequest();
            String messageKey = event.paymentId().toString();
            RiskCheckRequest request = toAvroRequest(event);
            riskCheckKafkaTemplate.send(topic, messageKey, request);
            log.info("Successfully published RiskCheckRequest for payment: {}", event.paymentId());
        } catch (Exception e) {
            log.error("Error publishing RiskCheckRequest for payment: {}", event.paymentId(), e);
            throw e;
        }
    }
}
