package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.risk.RiskAssessmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentCompletedEventProducer {

    private static final String RISK_ASSESSMENT_COMPLETED_IDEMPOTENCE_KEY_PREFIX = "risk-assessment-completed:";

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final OutboxEventPublisher outboxEventPublisher;

    public void produceRiskAssessmentCompletedEvent(RiskAssessmentCompletedEvent riskAssessmentCompletedEvent) {
        outboxEventPublisher.publish(
                kafkaConfigurationProperties.getTopics().getOutgoing().getRiskAssessmentCompleted(),
                riskAssessmentCompletedEvent.getPaymentId(),
                riskAssessmentCompletedEvent,
                RISK_ASSESSMENT_COMPLETED_IDEMPOTENCE_KEY_PREFIX + riskAssessmentCompletedEvent.getRiskCheckRequestId());

        log.info("Stored RiskAssessmentCompletedEvent in outbox for paymentId: {} and riskCheckRequestId: {}",
                riskAssessmentCompletedEvent.getPaymentId(),
                riskAssessmentCompletedEvent.getRiskCheckRequestId());
    }
}
