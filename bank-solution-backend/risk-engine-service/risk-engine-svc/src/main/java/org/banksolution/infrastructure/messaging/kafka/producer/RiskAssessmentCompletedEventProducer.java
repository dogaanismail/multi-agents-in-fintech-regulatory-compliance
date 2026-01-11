package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskAssessmentCompletedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.exception.RiskAssessmentCompletedEventException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentCompletedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull RiskAssessmentCompletedEvent> riskAssessmentCompletedEventKafkaTemplate;

    public void produceRiskAssessmentCompletedEvent(RiskAssessmentCompletedEvent event) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getRiskAssessmentCompleted();
            String messageKey = event.getPaymentId();

            riskAssessmentCompletedEventKafkaTemplate.send(topic, messageKey, event);
            log.info("Successfully published RiskAssessmentCompletedEvent for paymentId: {} and riskCheckRequestId: {}",
                    event.getPaymentId(),
                    event.getRiskCheckRequestId());
        } catch (Exception e) {
            log.error("Error publishing RiskAssessmentCompletedEvent for paymentId: {} and riskCheckRequestId: {}",
                    event.getPaymentId(),
                    event.getRiskCheckRequestId(),
                    e);
            throw new RiskAssessmentCompletedEventException(
                    "Failed to process FraudAnalysisCompletedEvent for paymentId: %s, riskCheckRequestId: %s",
                    e,
                    event.getPaymentId(),
                    event.getRiskCheckRequestId()
            );
        }
    }
}
