package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskCheckRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.payment.event.RiskCheckRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static org.banksolution.infrastructure.messaging.kafka.mapper.RiskCheckRequestMapper.toAvroRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckRequestProducer {

    private final KafkaTemplate<@NonNull String, @NonNull RiskCheckRequest> riskCheckKafkaTemplate;

    @Value("${kafka.topics.risk-check-request}")
    private String riskCheckRequestTopic;

    public void publishRiskCheckRequestedEvent(RiskCheckRequestedEvent event) {
        try {
            RiskCheckRequest request = toAvroRequest(event);
            riskCheckKafkaTemplate.send(riskCheckRequestTopic, event.referenceNumber(), request);
            log.info("Successfully published RiskCheckRequest for payment: {}", event.paymentId());
        } catch (Exception e) {
            log.error("Error publishing RiskCheckRequest for payment: {}", event.paymentId(), e);
            throw e;
        }
    }
}
