package org.banksolution.scheduling.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerDefaultsEnvironmentPostProcessorTest {

    private final SchedulerDefaultsEnvironmentPostProcessor schedulerDefaultsEnvironmentPostProcessor =
            new SchedulerDefaultsEnvironmentPostProcessor();

    @Test
    void shouldClaimDueExecutionsWithSkipLockedAndDetectDeadInstancesQuickly() {
        StandardEnvironment standardEnvironment = new StandardEnvironment();

        schedulerDefaultsEnvironmentPostProcessor.postProcessEnvironment(standardEnvironment, new SpringApplication());

        assertThat(standardEnvironment.getProperty("db-scheduler.polling-strategy")).isEqualTo("lock-and-fetch");
        assertThat(standardEnvironment.getProperty("db-scheduler.heartbeat-interval")).isEqualTo("30s");
        assertThat(standardEnvironment.getProperty("db-scheduler.delay-startup-until-context-ready")).isEqualTo("true");
    }

    @Test
    void shouldLetTheServiceOverrideEveryDefault() {
        StandardEnvironment standardEnvironment = new StandardEnvironment();
        standardEnvironment.getPropertySources().addFirst(new MapPropertySource("applicationProperties",
                Map.of("db-scheduler.polling-strategy", "fetch")));

        schedulerDefaultsEnvironmentPostProcessor.postProcessEnvironment(standardEnvironment, new SpringApplication());

        assertThat(standardEnvironment.getProperty("db-scheduler.polling-strategy")).isEqualTo("fetch");
        assertThat(standardEnvironment.getPropertySources().stream().toList().getLast().getName())
                .isEqualTo(SchedulerDefaultsEnvironmentPostProcessor.SCHEDULER_DEFAULTS_PROPERTY_SOURCE_NAME);
    }
}
