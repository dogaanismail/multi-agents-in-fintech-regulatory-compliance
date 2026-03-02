package org.banksolution.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.TaskInstanceId;
import com.github.kagkarlsson.scheduler.task.helper.RecurringTaskWithPersistentSchedule;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.scheduler.CurrencyConversionScheduledService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Configuration
@Slf4j
public class SchedulerConfig {

    private static final String TASK_NAME = "currency-rates-refresh";
    private static final String SCHEDULER_NAME = "payment-service-scheduler";

    @Value("${app.exchange-rate.scheduler.interval-ms:60000}")
    private long intervalMs;

    private Scheduler scheduler;

    @Bean
    public RecurringTaskWithPersistentSchedule<ScheduleAndRateData> currencyRatesRefreshTask(
            CurrencyConversionScheduledService service) {
        return Tasks.recurringWithPersistentSchedule(TASK_NAME, ScheduleAndRateData.class)
                .execute((instance, ctx) -> service.fetchAndUpdateRates());
    }

    @Bean
    public Scheduler scheduler(
            DataSource dataSource,
            RecurringTaskWithPersistentSchedule<ScheduleAndRateData> currencyRatesRefreshTask) {

        this.scheduler = Scheduler.create(dataSource, currencyRatesRefreshTask)
                .schedulerName(new SchedulerName.Fixed(SCHEDULER_NAME))
                .serializer(new JacksonSerializer())
                .enableImmediateExecution()
                .build();
        this.scheduler.start();

        TaskInstanceId instanceId = TaskInstanceId.of(TASK_NAME, "main");
        if (this.scheduler.getScheduledExecution(instanceId).isEmpty()) {
            this.scheduler.schedule(
                    SchedulableInstance.of(
                            currencyRatesRefreshTask.instance("main", new ScheduleAndRateData(intervalMs)),
                            Instant.now()
                    )
            );
            log.info("Currency rates refresh task scheduled (intervalMs={})", intervalMs);
        } else {
            log.info("Currency rates refresh task already exists in DB, using persisted schedule");
        }

        return this.scheduler;
    }

    @PreDestroy
    public void stopScheduler() {
        if (scheduler != null) {
            scheduler.stop();
        }
    }

    @Bean
    public DbSchedulerCustomizer dbSchedulerCustomizer() {
        return new DbSchedulerCustomizer() {
            @Override
            public Optional<SchedulerName> schedulerName() {
                return Optional.of(new SchedulerName.Fixed(SCHEDULER_NAME));
            }

            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(new JacksonSerializer());
            }
        };
    }

    @Getter
    public static class ScheduleAndRateData implements ScheduleAndData {

        private final long intervalMs;

        @JsonCreator
        public ScheduleAndRateData(@JsonProperty("intervalMs") long intervalMs) {
            this.intervalMs = intervalMs;
        }

        @Override
        public FixedDelay getSchedule() {
            return Schedules.fixedDelay(Duration.ofMillis(intervalMs));
        }

        @Override
        public Object getData() {
            return null;
        }
    }
}
