package org.banksolution.infrastructure.messaging.kafka.snapshot;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.annotation.MessageHandler;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
import org.banksolution.infrastructure.messaging.kafka.mapper.PaymentAggregateSnapshotMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Intercepts Axon aggregates snapshots and publishes them to Kafka.
 * <p>
 * <b>When is this triggered?</b>
 * This @MessageHandler is invoked by Axon every time a snapshot is created.
 * Since we configured EventCountSnapshotTriggerDefinition with count=1,
 * a snapshot is created AFTER EVERY EVENT applied to PaymentAggregate.
 * <p>
 * <b>What's in the Message parameter?</b>
 * The Message contains:
 * - Metadata: Snapshot timestamp, correlation IDs, causation IDs
 * - Headers: Aggregate type, aggregate identifier, sequence number
 * - Payload type information: The snapshot type (AggregateSnapshot)
 * <p>
 * We use the metadata to enrich our logging with Axon's internal tracking info.
 * <p>
 * <b>Flow:</b>
 * 1. Command handled → Event applied → Aggregate state updated
 * 2. EventCountSnapshotTriggerDefinition detects count threshold (1)
 * 3. Axon creates a snapshot of the current aggregate state
 * 4. This handler intercepts the snapshot
 * 5. We map aggregate to Avro PaymentSnapshotEvent
 * 6. Publish to Kafka for payment-history service
 * <p>
 * Benefits:
 * - Leverages Axon's native snapshot mechanism
 * - No custom snapshot building logic needed
 * - Snapshots represent actual aggregate state
 * - Automatic consistency with aggregate
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotEventPublisher {

    private final KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotKafkaTemplate;

    @Value("${kafka.topics.payment-snapshot-events}")
    private String snapshotTopic;

    @MessageHandler
    public void on(PaymentAggregate aggregate, Message<?> message) {
        try {
            String aggregateId = aggregate.getPaymentId().toString();

            PaymentSnapshotEvent snapshotEvent = PaymentAggregateSnapshotMapper.toSnapshot(aggregate);

            paymentSnapshotKafkaTemplate.send(snapshotTopic, aggregateId, snapshotEvent);
            log.info("Published snapshot to Kafka topic '{}' for payment: {}", snapshotTopic, aggregateId);
        } catch (Exception e) {
            log.error("Failed to publish aggregate snapshot for payment: {}",
                    aggregate.getPaymentId(), e);
        }
    }
}
