package org.banksolution.outbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.banksolution.outbox.common.kafka.KafkaTestConsumers.awaitMatchingRecord;
import static org.banksolution.outbox.common.kafka.KafkaTestConsumers.readHeader;
import static org.banksolution.outbox.fixtures.OutboxFixtures.PAYMENT_CREATED_TOPIC;
import static org.banksolution.outbox.fixtures.OutboxFixtures.createPaymentCreatedEvent;

import com.aml.payment.PaymentCreatedEvent;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.banksolution.outbox.common.annotations.OutboxIntegrationTest;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.banksolution.outbox.relay.OutboxKafkaSender;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

@OutboxIntegrationTest
class OutboxEventPublisherIntegrationTest {

    private static final Duration DELIVERY_TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Tracer tracer;

    @Test
    void shouldPublishTheEventToKafkaAfterCommitWithItsHeaders() {
        UUID paymentId = UUID.randomUUID();
        PaymentCreatedEvent paymentCreatedEvent = createPaymentCreatedEvent(paymentId);
        String idempotenceKey = "payment-created:" + paymentId;

        UUID outboxEventId = transactionTemplate.execute(transactionStatus -> outboxEventPublisher.publish(
                PAYMENT_CREATED_TOPIC, paymentId.toString(), paymentCreatedEvent, idempotenceKey));

        Optional<ConsumerRecord<String, Object>> consumerRecord = awaitMatchingRecord(
                PAYMENT_CREATED_TOPIC, DELIVERY_TIMEOUT, matchingRecord -> paymentId.toString().equals(matchingRecord.key()));
        assertThat(consumerRecord).isPresent();
        GenericRecord deliveredPayload = (GenericRecord) consumerRecord.get().value();
        assertThat(deliveredPayload.get("paymentId")).hasToString(paymentId.toString());
        assertThat(deliveredPayload.get("eventId")).hasToString(paymentCreatedEvent.getEventId());
        assertThat(readHeader(consumerRecord.get(), OutboxKafkaSender.OUTBOX_EVENT_ID_HEADER)).isEqualTo(outboxEventId.toString());
        assertThat(readHeader(consumerRecord.get(), OutboxKafkaSender.IDEMPOTENCE_KEY_HEADER)).isEqualTo(idempotenceKey);
        assertThat(readHeader(consumerRecord.get(), OutboxKafkaSender.PAYLOAD_TYPE_HEADER)).isEqualTo("com.aml.payment.PaymentCreatedEvent");
        await().atMost(DELIVERY_TIMEOUT).untilAsserted(() ->
                assertThat(outboxEventRepository.findById(outboxEventId).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.PROCESSED));
    }

    @Test
    void shouldStoreAndPublishNothingWhenTheTransactionRollsBack() {
        UUID paymentId = UUID.randomUUID();

        UUID outboxEventId = transactionTemplate.execute(transactionStatus -> {
            UUID publishedOutboxEventId = outboxEventPublisher.publish(
                    PAYMENT_CREATED_TOPIC, paymentId.toString(), createPaymentCreatedEvent(paymentId), null);
            transactionStatus.setRollbackOnly();
            return publishedOutboxEventId;
        });

        assertThat(outboxEventRepository.findById(outboxEventId)).isEmpty();
        assertThat(awaitMatchingRecord(PAYMENT_CREATED_TOPIC, Duration.ofSeconds(3),
                matchingRecord -> paymentId.toString().equals(matchingRecord.key()))).isEmpty();
    }

    @Test
    void shouldRefuseToPublishOutsideATransaction() {
        UUID paymentId = UUID.randomUUID();
        PaymentCreatedEvent paymentCreatedEvent = createPaymentCreatedEvent(paymentId);
        String referenceId = paymentId.toString();

        assertThatThrownBy(() -> outboxEventPublisher.publish(PAYMENT_CREATED_TOPIC, referenceId, paymentCreatedEvent, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void shouldContinueTheCallersTraceOnTheKafkaRecord() {
        UUID paymentId = UUID.randomUUID();
        Span callerSpan = tracer.nextSpan().name("request payment").start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(callerSpan)) {
            transactionTemplate.executeWithoutResult(transactionStatus -> outboxEventPublisher.publish(
                    PAYMENT_CREATED_TOPIC, paymentId.toString(), createPaymentCreatedEvent(paymentId), null));
        } finally {
            callerSpan.end();
        }

        Optional<ConsumerRecord<String, Object>> consumerRecord = awaitMatchingRecord(
                PAYMENT_CREATED_TOPIC, DELIVERY_TIMEOUT, matchingRecord -> paymentId.toString().equals(matchingRecord.key()));
        assertThat(consumerRecord).isPresent();
        assertThat(readHeader(consumerRecord.get(), "traceparent")).contains(callerSpan.context().traceId());
    }
}
