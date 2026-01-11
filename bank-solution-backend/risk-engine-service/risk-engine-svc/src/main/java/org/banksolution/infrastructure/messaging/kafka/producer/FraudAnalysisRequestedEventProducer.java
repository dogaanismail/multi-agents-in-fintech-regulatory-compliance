package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisRequestedEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull FraudAnalysisRequestedEvent> fraudAnalysisRequestedEventKafkaTemplate;

    public void publishFraudAnalysisRequestedEvent(FraudAnalysisRequestedEvent event) {
        try {
            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getFraudAnalysisRequested();
            String messageKey = event.getPaymentId();

            fraudAnalysisRequestedEventKafkaTemplate.send(topic, messageKey, event);
            log.info("Successfully published FraudAnalysisRequestedEvent for paymentId: {} and riskCheckRequestId: {}",
                    event.getPaymentId(),
                    event.getRiskCheckRequestId());
        } catch (Exception e) {
            log.error("Error publishing FraudAnalysisRequestedEvent for paymentId: {} and riskCheckRequestId: {}",
                    event.getPaymentId(),
                    event.getRiskCheckRequestId(),
                    e);
            throw e;
        }
    }
}
