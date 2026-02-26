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
import org.banksolution.service.GraphAlgorithmScheduledService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Configuration
@Slf4j
public class SchedulerConfig {

    private static final String SCHEDULER_NAME = "network-topology-scheduler";

    @Value("${app.graph.algorithm.interval-ms:300000}")
    private long intervalMs;

    private Scheduler scheduler;

    @Bean
    public RecurringTaskWithPersistentSchedule<ScheduleAndNoData> graphAlgorithmTask(GraphAlgorithmScheduledService service) {
        return Tasks.recurringWithPersistentSchedule(SCHEDULER_NAME, ScheduleAndNoData.class)
                .execute((instance, ctx) -> service.computeAllMetrics());
    }

    @Bean
    @DependsOn("liquibase")
    public Scheduler scheduler(DataSource dataSource, RecurringTaskWithPersistentSchedule<ScheduleAndNoData> graphAlgorithmTask) {
        this.scheduler = Scheduler.create(dataSource, graphAlgorithmTask)
                .schedulerName(new SchedulerName.Fixed(SCHEDULER_NAME))
                .serializer(new JacksonSerializer())
                .enableImmediateExecution()
                .build();
        this.scheduler.start();

        TaskInstanceId instanceId = TaskInstanceId.of(SCHEDULER_NAME, "main");
        if (this.scheduler.getScheduledExecution(instanceId).isEmpty()) {
            this.scheduler.schedule(
                    SchedulableInstance.of(
                            graphAlgorithmTask.instance("main", new ScheduleAndNoData(intervalMs)),
                            Instant.now()
                    )
            );
            log.info("Graph algorithm task scheduled (intervalMs={})", intervalMs);
        } else {
            log.info("Graph algorithm task already exists in DB, using persisted schedule");
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
    public static class ScheduleAndNoData implements ScheduleAndData {

        private final long intervalMs;

        @JsonCreator
        public ScheduleAndNoData(@JsonProperty("intervalMs") long intervalMs) {
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
