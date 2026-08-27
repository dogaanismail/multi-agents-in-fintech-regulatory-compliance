package org.banksolution.kafka;

import com.aml.payment.PaymentSnapshotEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.entity.PaymentHistoryEntity;
import org.banksolution.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.fixtures.PaymentHistoryFixtures.*;

class PaymentSnapshotProjectionFlowTest extends BaseIntegrationTest {

    private static final Duration PROJECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEAD_LETTER_TIMEOUT = Duration.ofSeconds(60);
    private static final String DEAD_LETTER_TOPIC_SUFFIX = ".DLT";

    @Value("${spring.kafka.topics.incoming.payment-snapshot-events}")
    private String paymentSnapshotTopic;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Test
    void shouldProjectEachSnapshotOntoOneRowAndKeepTheLatestState() throws Exception {
        UUID paymentId = UUID.randomUUID();

        publish(createInitiatedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(paymentHistoryRepository.findById(paymentId)).map(PaymentHistoryEntity::getStatus).contains("INITIATED"));

        publish(createCompletedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));
        publish(createCompletedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));
        // a replayed older snapshot must not roll the projection back
        publish(createInitiatedPaymentSnapshotEvent(paymentId, CUSTOMER_ID));

        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() -> {
            PaymentHistoryEntity paymentHistoryEntity = paymentHistoryRepository.findById(paymentId).orElseThrow();
            assertThat(paymentHistoryEntity.getStatus()).isEqualTo("COMPLETED");
            // the redelivered identical snapshot changes nothing, so no second UPDATE is issued
            assertThat(paymentHistoryEntity.getEntityVersion()).isEqualTo((short) 1);
            assertThat(paymentHistoryEntity.getMarlAssessment().getAction()).isEqualTo("BLOCK");
            assertThat(paymentHistoryEntity.getCompletedAt()).isEqualTo(COMPLETED_AT);
            assertThat(paymentHistoryEntity.getAggregateVersion()).isEqualTo(7);
        });

        assertThat(paymentHistoryRepository.findByCustomerId(CUSTOMER_ID, org.springframework.data.domain.PageRequest.of(0, 100)).getContent())
                .filteredOn(paymentHistoryEntity -> paymentHistoryEntity.getPaymentId().equals(paymentId))
                .hasSize(1);
    }

    @Test
    void shouldParkASnapshotWithAMalformedPaymentIdOnTheDeadLetterTopicWithoutRetrying() throws Exception {
        String malformedPaymentId = "not-a-uuid-" + UUID.randomUUID();
        PaymentSnapshotEvent malformedPaymentSnapshotEvent = createMalformedPaymentSnapshotEvent(malformedPaymentId);

        try (KafkaProducer<String, PaymentSnapshotEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(paymentSnapshotTopic, malformedPaymentId, malformedPaymentSnapshotEvent)).get();
        }

        PaymentSnapshotEvent parkedPaymentSnapshotEvent = KafkaTestClients.awaitMatchingEvent(
                paymentSnapshotTopic + DEAD_LETTER_TOPIC_SUFFIX,
                DEAD_LETTER_TIMEOUT,
                (PaymentSnapshotEvent deadLetteredEvent) -> malformedPaymentId.equals(deadLetteredEvent.getPaymentId()));
        assertThat(parkedPaymentSnapshotEvent.getReferenceNumber()).isEqualTo(malformedPaymentSnapshotEvent.getReferenceNumber());
    }

    private void publish(PaymentSnapshotEvent paymentSnapshotEvent) throws ExecutionException, InterruptedException {
        try (KafkaProducer<String, PaymentSnapshotEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(paymentSnapshotTopic, paymentSnapshotEvent.getPaymentId(), paymentSnapshotEvent)).get();
        }
    }
}
