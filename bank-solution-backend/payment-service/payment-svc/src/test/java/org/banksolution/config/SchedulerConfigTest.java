package org.banksolution.config;

import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.task.schedule.FixedDelay;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerConfigTest {

    @Test
    void shouldPinTheSchedulerNameAndUseJacksonForTaskData() {
        DbSchedulerCustomizer dbSchedulerCustomizer = new SchedulerConfig().dbSchedulerCustomizer();

        assertThat(dbSchedulerCustomizer.schedulerName()).map(SchedulerName::getName).contains("payment-service-scheduler");
        assertThat(dbSchedulerCustomizer.serializer()).containsInstanceOf(JacksonSerializer.class);
    }

    @Test
    void shouldTurnTheConfiguredIntervalIntoAFixedDelayScheduleWithoutPayload() {
        SchedulerConfig.ScheduleAndRateData scheduleAndRateData = new SchedulerConfig.ScheduleAndRateData(60_000L);

        assertThat(scheduleAndRateData.getIntervalMs()).isEqualTo(60_000L);
        assertThat(scheduleAndRateData.getSchedule()).isEqualTo(FixedDelay.of(Duration.ofMillis(60_000L)));
        assertThat(scheduleAndRateData.getData()).isNull();
    }

    @Test
    void shouldTolerateStoppingBeforeTheSchedulerWasEverBuilt() {
        SchedulerConfig schedulerConfig = new SchedulerConfig();

        org.assertj.core.api.Assertions.assertThatCode(schedulerConfig::stopScheduler).doesNotThrowAnyException();
    }
}
