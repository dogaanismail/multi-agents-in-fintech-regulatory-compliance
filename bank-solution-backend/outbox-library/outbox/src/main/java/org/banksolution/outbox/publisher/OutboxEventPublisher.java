package org.banksolution.outbox.publisher;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.banksolution.outbox.entity.OutboxEventEntity;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.banksolution.outbox.relay.OutboxEventRelay;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.banksolution.outbox.serialization.OutboxPayloadSerializer;
import org.banksolution.outbox.tracing.OutboxTraceContextPropagator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public class OutboxEventPublisher implements DisposableBean {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadSerializer outboxPayloadSerializer;
    private final OutboxTraceContextPropagator outboxTraceContextPropagator;
    private final OutboxEventRelay outboxEventRelay;
    private final ThreadPoolTaskExecutor immediatePublishExecutor;
    private final Clock clock;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxPayloadSerializer outboxPayloadSerializer,
            OutboxTraceContextPropagator outboxTraceContextPropagator,
            OutboxEventRelay outboxEventRelay,
            ThreadPoolTaskExecutor immediatePublishExecutor,
            Clock clock) {

        this.outboxEventRepository = outboxEventRepository;
        this.outboxPayloadSerializer = outboxPayloadSerializer;
        this.outboxTraceContextPropagator = outboxTraceContextPropagator;
        this.outboxEventRelay = outboxEventRelay;
        this.immediatePublishExecutor = immediatePublishExecutor;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID publish(
            String destination,
            String referenceId,
            SpecificRecord payload,
            String idempotenceKey) {

        Instant createdAt = clock.instant();
        OutboxEventEntity outboxEventEntity = OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .referenceId(referenceId)
                .destination(destination)
                .payloadType(payload.getSchema().getFullName())
                .payload(outboxPayloadSerializer.serializeOutboxPayload(destination, payload))
                .idempotenceKey(idempotenceKey)
                .headers(outboxTraceContextPropagator.captureCurrentTraceHeaders())
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(createdAt)
                .createdAt(createdAt)
                .build();

        outboxEventRepository.save(outboxEventEntity);
        publishImmediatelyAfterCommit(outboxEventEntity.getId());
        return outboxEventEntity.getId();
    }

    @Override
    public void destroy() {
        if (immediatePublishExecutor != null) {
            immediatePublishExecutor.shutdown();
        }
    }

    private void publishImmediatelyAfterCommit(UUID outboxEventId) {
        if (immediatePublishExecutor == null) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    immediatePublishExecutor.execute(() -> outboxEventRelay.relayPendingOutboxEvent(outboxEventId));
                } catch (TaskRejectedException _) {
                    log.debug("Immediate publish queue full, outbox event {} is left for the relay", outboxEventId);
                }
            }
        });
    }
}
