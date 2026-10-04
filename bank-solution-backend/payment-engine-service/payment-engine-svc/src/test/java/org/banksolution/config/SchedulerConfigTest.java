package org.banksolution.config;

import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import org.banksolution.infrastructure.deadletter.DeadLetterRetryScheduler;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SchedulerConfigTest {

    private final SchedulerConfig schedulerConfig = new SchedulerConfig();

    @Test
    void shouldNameEachReplicaAndKeepJacksonForAxonsPersistedTaskData() {
        DbSchedulerCustomizer dbSchedulerCustomizer = schedulerConfig.dbSchedulerCustomizer("payment-engine-svc");

        assertThat(dbSchedulerCustomizer.schedulerName())
                .map(SchedulerName::getName)
                .hasValueSatisfying(schedulerInstanceName -> assertThat(schedulerInstanceName).startsWith("payment-engine-svc@"));
        assertThat(dbSchedulerCustomizer.serializer()).containsInstanceOf(JacksonSerializer.class);
    }

    @Test
    void shouldDrainTheDeadLetterQueuesAsOneRecurringInstance() {
        DeadLetterRetryScheduler deadLetterRetryScheduler = mock(DeadLetterRetryScheduler.class);
        RecurringTask<Void> deadLetterRetryTask = schedulerConfig.deadLetterRetryTask(deadLetterRetryScheduler, Duration.ofSeconds(10));

        deadLetterRetryTask.executeRecurringly(null, null);

        assertThat(deadLetterRetryTask.getDefaultTaskInstance())
                .isEqualTo(TaskInstanceId.of(SchedulerConfig.DEAD_LETTER_RETRY_TASK_NAME, RecurringTask.INSTANCE));
        verify(deadLetterRetryScheduler).retryDeadLetters();
    }
}
