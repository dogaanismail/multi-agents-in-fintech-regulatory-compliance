package org.banksolution.outbox.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.outbox.fixtures.OutboxFixtures.RAW_PAYLOAD_TOPIC;
import static org.banksolution.outbox.fixtures.OutboxFixtures.createOutboxEventEntity;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;

import java.time.Duration;
import java.time.Instant;

import org.banksolution.outbox.common.annotations.OutboxIntegrationTest;
import org.banksolution.outbox.entity.OutboxEventEntity;
import org.banksolution.outbox.enums.OutboxEventStatus;
import org.banksolution.outbox.repository.OutboxEventRepository;
import org.banksolution.outbox.scheduling.OutboxSchedulerTasks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@OutboxIntegrationTest
class OutboxSchedulerIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SchedulerClient schedulerClient;

    @Test
    void shouldRelayAnEventTheImmediatePathNeverSawWhenTheRelayTaskRuns() {
        OutboxEventEntity outboxEventEntity = outboxEventRepository.save(
                createOutboxEventEntity(RAW_PAYLOAD_TOPIC, OutboxEventStatus.RETRY, Instant.now()));

        schedulerClient.reschedule(
                TaskInstanceId.of(OutboxSchedulerTasks.OUTBOX_RELAY_TASK_NAME, RecurringTask.INSTANCE),
                Instant.now());

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
        {
            assert outboxEventEntity.getId() != null;
            assertThat(outboxEventRepository.findById(outboxEventEntity.getId()).orElseThrow().getStatus())
                    .isEqualTo(OutboxEventStatus.PROCESSED);
        });
    }
}
