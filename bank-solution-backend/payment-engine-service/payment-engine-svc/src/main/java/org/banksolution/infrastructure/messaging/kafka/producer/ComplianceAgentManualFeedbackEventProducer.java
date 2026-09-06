package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.infrastructure.messaging.kafka.mapper.ComplianceAgentManualFeedbackEventMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComplianceAgentManualFeedbackEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull ComplianceAgentManualFeedbackEvent> agentManualFeedbackEventKafkaTemplate;

    public void publish(
            String paymentId,
            String feedbackType,
            String originalMarlAction,
            String officerDecision,
            String reviewedBy,
            String notes
    ) {
        log.debug("Publishing ComplianceAgentManualFeedbackEvent for paymentId: {}", paymentId);

        ComplianceAgentManualFeedbackEvent complianceAgentManualFeedbackEvent = ComplianceAgentManualFeedbackEventMapper.toAvroEvent(
                paymentId,
                feedbackType,
                originalMarlAction,
                officerDecision,
                reviewedBy,
                notes
        );

        String agentManualFeedbackTopic = kafkaConfigurationProperties.getTopics().getOutgoing().getAgentManualFeedback();
        KafkaDeliveryAwaiter.awaitDelivery(
                agentManualFeedbackEventKafkaTemplate.send(agentManualFeedbackTopic, paymentId, complianceAgentManualFeedbackEvent),
                agentManualFeedbackTopic,
                paymentId);
        log.info("Successfully published ComplianceAgentManualFeedbackEvent: paymentId={}, feedbackType={}, officerDecision={}",
                paymentId, feedbackType, officerDecision);
    }
}
