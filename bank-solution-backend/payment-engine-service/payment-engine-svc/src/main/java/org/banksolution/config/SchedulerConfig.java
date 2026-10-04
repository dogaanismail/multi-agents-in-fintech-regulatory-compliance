package org.banksolution.config;

import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.banksolution.infrastructure.deadletter.DeadLetterRetryScheduler;
import org.banksolution.scheduling.config.SchedulerInstanceNameCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;

@Configuration
public class SchedulerConfig {

    public static final String DEAD_LETTER_RETRY_TASK_NAME = "dead-letter-retry";

    /**
     * Axon's deadline and event-scheduling rows already in scheduled_tasks were written with Jackson;
     * the starter's Java-serialization default could not read them back.
     */
    @Bean
    public DbSchedulerCustomizer dbSchedulerCustomizer(@Value("${spring.application.name}") String applicationName) {
        return new SchedulerInstanceNameCustomizer(applicationName) {
            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(new JacksonSerializer());
            }
        };
    }

    @Bean
    public RecurringTask<Void> deadLetterRetryTask(
            DeadLetterRetryScheduler deadLetterRetryScheduler,
            @Value("${payment-engine.dead-letter.retry-interval}") Duration deadLetterRetryInterval) {

        return Tasks.recurring(DEAD_LETTER_RETRY_TASK_NAME, FixedDelay.of(deadLetterRetryInterval))
                .execute((_, _) -> deadLetterRetryScheduler.retryDeadLetters());
    }
}
