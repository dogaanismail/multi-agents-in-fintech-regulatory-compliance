package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.infrastructure.messaging.kafka.mapper.ComplianceAgentManualFeedbackEventMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ComplianceAgentManualFeedbackEventProducer {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final KafkaTemplate<@NonNull String, @NonNull ComplianceAgentManualFeedbackEvent> agentManualFeedbackEventKafkaTemplate;

    @Async
    public void publish(
            String paymentId,
            String feedbackType,
            String originalMarlAction,
            String officerDecision,
            String reviewedBy,
            String notes
    ) {
        try {
            log.debug("Publishing ComplianceAgentManualFeedbackEvent for paymentId: {}", paymentId);

            ComplianceAgentManualFeedbackEvent event = ComplianceAgentManualFeedbackEventMapper.toAvroEvent(
                    paymentId,
                    feedbackType,
                    originalMarlAction,
                    officerDecision,
                    reviewedBy,
                    notes
            );

            String topic = kafkaConfigurationProperties.getTopics().getOutgoing().getAgentManualFeedback();
            agentManualFeedbackEventKafkaTemplate.send(topic, paymentId, event);
            log.info("Successfully published ComplianceAgentManualFeedbackEvent: paymentId={}, feedbackType={}, officerDecision={}",
                    paymentId, feedbackType, officerDecision);
        } catch (Exception e) {
            log.error("Failed to publish ComplianceAgentManualFeedbackEvent for paymentId: {}", paymentId, e);
        }
    }
}
