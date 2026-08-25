package org.banksolution.config;

import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerConfigTest {

    @Test
    void shouldPinTheSchedulerNameAndUseJacksonForTaskData() {
        DbSchedulerCustomizer dbSchedulerCustomizer = new SchedulerConfig().customizer();

        assertThat(dbSchedulerCustomizer.schedulerName()).map(SchedulerName::getName).contains("spring-boot-scheduler-1");
        assertThat(dbSchedulerCustomizer.serializer()).containsInstanceOf(JacksonSerializer.class);
    }
}
