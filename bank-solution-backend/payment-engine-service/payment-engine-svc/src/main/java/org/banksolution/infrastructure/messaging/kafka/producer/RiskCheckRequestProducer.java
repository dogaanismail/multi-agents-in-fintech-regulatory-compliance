package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskCheckRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.payment.event.RiskCheckRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskCheckRequestProducer {

    private final KafkaTemplate<@NonNull String, @NonNull RiskCheckRequest> riskCheckKafkaTemplate;

    @Value("${kafka.topics.risk-check-request}")
    private String riskCheckRequestTopic;

    public void publishRiskCheckRequest(RiskCheckRequestedEvent event) {
        try {
            RiskCheckRequest request = RiskCheckRequest.newBuilder()
                    .setRequestId(UUID.randomUUID().toString())
                    .setPaymentId(event.getPaymentId().toString())
                    .setReferenceNumber(event.getReferenceNumber())
                    .setCustomerId(event.getCustomerId().toString())
                    .setSourceAccountId(event.getSourceAccountId() != null ? event.getSourceAccountId().toString() : null)
                    .setDestinationAccountId(event.getDestinationAccountId() != null ? event.getDestinationAccountId().toString() : null)
                    .setAmount(event.getAmount().toString())
                    .setCurrency(event.getCurrency())
                    .setPaymentType(PaymentType.valueOf(event.getPaymentType()))
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setDescription(event.getDescription())
                    .build();

            riskCheckKafkaTemplate.send(riskCheckRequestTopic, event.getReferenceNumber(), request);
            log.info("Successfully published RiskCheckRequest for payment: {}", event.getPaymentId());
        } catch (Exception e) {
            log.error("Error publishing RiskCheckRequest for payment: {}", event.getPaymentId(), e);
            throw new RuntimeException("Failed to publish risk check request", e);
        }
    }
}
