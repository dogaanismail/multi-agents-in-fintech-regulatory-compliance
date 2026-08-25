package org.banksolution.infrastructure.messaging.kafka;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.repository.MarlAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;

class FraudAnalysisCompletedDeadLetterTest extends BaseIntegrationTest {

    /**
     * The error handler retries 3 times with exponential backoff (1s/2s/4s) before
     * parking the message, so the DLT assertion needs a generous timeout.
     */
    private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";

    @Value("${spring.kafka.topics.incoming.fraud-analysis-completed}")
    private String fraudAnalysisCompletedTopic;

    @Autowired
    private MarlAssessmentRepository marlAssessmentRepository;

    @Test
    void shouldParkACompletionForAnUnknownRequestOnTheDeadLetterTopicAfterRetries()
            throws ExecutionException, InterruptedException {

        UUID unknownRiskCheckRequestId = UUID.randomUUID();
        String paymentId = "PAY-" + UUID.randomUUID();
        FraudAnalysisCompletedEvent poisonEvent =
                createFraudAnalysisCompletedEvent(unknownRiskCheckRequestId.toString(), paymentId);

        try (KafkaProducer<String, FraudAnalysisCompletedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(fraudAnalysisCompletedTopic, paymentId, poisonEvent)).get();
        }

        FraudAnalysisCompletedEvent parked = KafkaTestClients.awaitMatchingEvent(
                fraudAnalysisCompletedTopic + DEAD_LETTER_TOPIC_SUFFIX,
                DEAD_LETTER_TIMEOUT,
                deadLetteredEvent -> unknownRiskCheckRequestId.toString()
                        .equals(deadLetteredEvent.getRiskCheckRequestId()));

        assertThat(parked.getPaymentId()).isEqualTo(paymentId);
        assertThat(marlAssessmentRepository.existsByRiskCheckRequestId(unknownRiskCheckRequestId)).isFalse();
    }
}
