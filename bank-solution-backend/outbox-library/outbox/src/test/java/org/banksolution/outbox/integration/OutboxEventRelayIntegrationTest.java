package org.banksolution.outbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.outbox.fixtures.OutboxFixtures.RAW_PAYLOAD_TOPIC;
import static org.banksolution.outbox.fixtures.OutboxFixtures.createOutboxEventEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.banksolution.outbox.common.annotations.OutboxIntegrationTest;
import org.banksolution.outbox.entity.OutboxEventEntity;
import org.banksolution.outbox.enums.OutboxEventFailType;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.banksolution.outbox.relay.OutboxEventCleaner;
import org.banksolution.outbox.relay.OutboxEventRelay;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@OutboxIntegrationTest
class OutboxEventRelayIntegrationTest {

    @Autowired
    private OutboxEventRelay outboxEventRelay;

    @Autowired
    private OutboxEventCleaner outboxEventCleaner;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void shouldSkipAnEventLockedByAnotherRelayAndPublishItOnceReleased() throws Exception {
        OutboxEventEntity outboxEventEntity = saveOutboxEvent(RAW_PAYLOAD_TOPIC, OutboxEventStatus.PENDING, Instant.now());
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        CompletableFuture<Void> otherRelayHoldingTheLock = CompletableFuture.runAsync(() ->
                transactionTemplate.executeWithoutResult(transactionStatus -> {
                    outboxEventRepository.claimPendingOutboxEvent(outboxEventEntity.getId());
                    lockAcquired.countDown();
                    awaitQuietly(releaseLock);
                }));
        assertThat(lockAcquired.await(10, TimeUnit.SECONDS)).isTrue();

        outboxEventRelay.relayPendingOutboxEvent(outboxEventEntity.getId());
        assert outboxEventEntity.getId() != null;
        assertThat(outboxEventRepository.findById(outboxEventEntity.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxEventStatus.PENDING);

        releaseLock.countDown();
        otherRelayHoldingTheLock.get(10, TimeUnit.SECONDS);
        outboxEventRelay.relayPendingOutboxEvent(outboxEventEntity.getId());
        assertThat(outboxEventRepository.findById(outboxEventEntity.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxEventStatus.PROCESSED);
    }

    @Test
    void shouldFailAnEventForAnInvalidTopicPermanentlyWithoutRetrying() {
        OutboxEventEntity outboxEventEntity = saveOutboxEvent("invalid topic name!", OutboxEventStatus.PENDING, Instant.now());

        outboxEventRelay.relayPendingOutboxEvent(outboxEventEntity.getId());

        assert outboxEventEntity.getId() != null;
        OutboxEventEntity failedOutboxEvent = outboxEventRepository.findById(outboxEventEntity.getId()).orElseThrow();
        assertThat(failedOutboxEvent.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(failedOutboxEvent.getFailType()).isEqualTo(OutboxEventFailType.PERMANENT);
        assertThat(failedOutboxEvent.getAttempts()).isEqualTo(1);
        assertThat(failedOutboxEvent.getFailReason()).isNotBlank();
    }

    @Test
    void shouldDeleteOnlyProcessedEventsOlderThanTheRetention() {
        Instant now = Instant.now();
        OutboxEventEntity expiredProcessedOutboxEvent = saveProcessedOutboxEvent(now.minus(Duration.ofDays(8)));
        OutboxEventEntity recentProcessedOutboxEvent = saveProcessedOutboxEvent(now.minus(Duration.ofDays(1)));
        OutboxEventEntity oldFailedOutboxEvent = saveOutboxEvent(RAW_PAYLOAD_TOPIC, OutboxEventStatus.FAILED, now.minus(Duration.ofDays(30)));

        outboxEventCleaner.deleteExpiredProcessedOutboxEvents();

        assert expiredProcessedOutboxEvent.getId() != null;
        assertThat(outboxEventRepository.findById(expiredProcessedOutboxEvent.getId())).isEmpty();

        assert recentProcessedOutboxEvent.getId() != null;
        assertThat(outboxEventRepository.findById(recentProcessedOutboxEvent.getId())).isPresent();

        assert oldFailedOutboxEvent.getId() != null;
        assertThat(outboxEventRepository.findById(oldFailedOutboxEvent.getId())).isPresent();
    }

    @Test
    void shouldPersistEveryStatusAndFailTypeTheSchemaAllows() {
        List<OutboxEventStatus> storedStatuses = Arrays.stream(OutboxEventStatus.values())
                .map(outboxEventStatus -> saveOutboxEvent(RAW_PAYLOAD_TOPIC, outboxEventStatus, Instant.now().plus(Duration.ofDays(1))))
                .map(savedOutboxEvent -> {
                    assert savedOutboxEvent.getId() != null;
                    return outboxEventRepository.findById(savedOutboxEvent.getId()).orElseThrow().getStatus();
                })
                .toList();
        List<OutboxEventFailType> storedFailTypes = Arrays.stream(OutboxEventFailType.values())
                .map(outboxEventFailType -> {
                    OutboxEventEntity outboxEventEntity = createOutboxEventEntity(
                            RAW_PAYLOAD_TOPIC, OutboxEventStatus.FAILED, Instant.now().plus(Duration.ofDays(1)));
                    outboxEventEntity.setFailType(outboxEventFailType);
                    return outboxEventRepository.save(outboxEventEntity);
                })
                .map(savedOutboxEvent -> {
                    assert savedOutboxEvent.getId() != null;
                    return outboxEventRepository.findById(savedOutboxEvent.getId()).orElseThrow().getFailType();
                })
                .toList();

        assertThat(storedStatuses).containsExactly(OutboxEventStatus.values());
        assertThat(storedFailTypes).containsExactly(OutboxEventFailType.values());
    }

    private OutboxEventEntity saveOutboxEvent(String destination, OutboxEventStatus outboxEventStatus, Instant nextAttemptAt) {
        return outboxEventRepository.save(createOutboxEventEntity(destination, outboxEventStatus, nextAttemptAt));
    }

    private OutboxEventEntity saveProcessedOutboxEvent(Instant processedAt) {
        OutboxEventEntity outboxEventEntity = createOutboxEventEntity(RAW_PAYLOAD_TOPIC, OutboxEventStatus.PROCESSED, processedAt);
        outboxEventEntity.setProcessedAt(processedAt);
        return outboxEventRepository.save(outboxEventEntity);
    }

    private static void awaitQuietly(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
