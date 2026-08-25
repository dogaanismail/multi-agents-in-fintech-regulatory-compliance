package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;
import org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentRequestedEventMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentRequestedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull RiskAssessmentRequestedEvent> riskAssessmentRequestedEventKafkaTemplate;

    public void publishRiskAssessmentRequestedEvent(RiskAssessmentInitiatedEvent riskAssessmentInitiatedEvent) {
        try {
            String riskAssessmentRequestedTopic = kafkaConfigurationProperties.getTopics().getOutgoing().getRiskAssessmentRequested();
            String messageKey = riskAssessmentInitiatedEvent.paymentId().toString();

            RiskAssessmentRequestedEvent riskAssessmentRequestedEvent = RiskAssessmentRequestedEventMapper.toAvroRequest(riskAssessmentInitiatedEvent);
            riskAssessmentRequestedEventKafkaTemplate.send(riskAssessmentRequestedTopic, messageKey, riskAssessmentRequestedEvent);

            log.info("Successfully published RiskAssessmentRequestedEvent for payment: {}", riskAssessmentInitiatedEvent.paymentId());
        } catch (Exception exception) {
            log.error("Error publishing RiskAssessmentRequestedEvent for payment: {}", riskAssessmentInitiatedEvent.paymentId(), exception);
            throw exception;
        }
    }
}
