package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisRequestedEventProducer {

    private static final String FRAUD_ANALYSIS_REQUESTED_IDEMPOTENCE_KEY_PREFIX = "fraud-analysis-requested:";

    private final KafkaConfigurationProperties kafkaConfigurationProperties;
    private final OutboxEventPublisher outboxEventPublisher;

    public void publishFraudAnalysisRequestedEvent(FraudAnalysisRequestedEvent fraudAnalysisRequestedEvent) {
        outboxEventPublisher.publish(
                kafkaConfigurationProperties.getTopics().getOutgoing().getFraudAnalysisRequested(),
                fraudAnalysisRequestedEvent.getPaymentId(),
                fraudAnalysisRequestedEvent,
                FRAUD_ANALYSIS_REQUESTED_IDEMPOTENCE_KEY_PREFIX + fraudAnalysisRequestedEvent.getRiskCheckRequestId());

        log.info("Stored FraudAnalysisRequestedEvent in outbox for paymentId: {} and riskCheckRequestId: {}",
                fraudAnalysisRequestedEvent.getPaymentId(),
                fraudAnalysisRequestedEvent.getRiskCheckRequestId());
    }
}
