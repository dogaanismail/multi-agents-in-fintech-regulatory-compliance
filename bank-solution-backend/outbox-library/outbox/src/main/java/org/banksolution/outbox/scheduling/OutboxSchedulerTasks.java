package org.banksolution.outbox.scheduling;

import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;

import java.time.Duration;

import org.banksolution.outbox.relay.OutboxEventCleaner;
import org.banksolution.outbox.relay.OutboxEventRelay;

public final class OutboxSchedulerTasks {

    public static final String OUTBOX_RELAY_TASK_NAME = "outbox-relay";
    public static final String OUTBOX_CLEANUP_TASK_NAME = "outbox-cleanup";

    private OutboxSchedulerTasks() {
    }

    public static RecurringTask<Void> createOutboxRelayTask(
            OutboxEventRelay outboxEventRelay,
            Duration pollingInterval) {

        return Tasks.recurring(OUTBOX_RELAY_TASK_NAME, FixedDelay.of(pollingInterval))
                .execute((_, _) -> outboxEventRelay.relayAllDueOutboxEvents());
    }

    public static RecurringTask<Void> createOutboxCleanupTask(
            OutboxEventCleaner outboxEventCleaner,
            Duration cleanupInterval) {

        return Tasks.recurring(OUTBOX_CLEANUP_TASK_NAME, FixedDelay.of(cleanupInterval))
                .execute((_, _) -> outboxEventCleaner.deleteExpiredProcessedOutboxEvents());
    }
}
