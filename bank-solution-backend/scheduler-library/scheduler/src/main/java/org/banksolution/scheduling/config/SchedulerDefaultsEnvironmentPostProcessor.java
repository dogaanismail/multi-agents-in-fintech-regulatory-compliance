package org.banksolution.scheduling.config;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class SchedulerDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    public static final String SCHEDULER_DEFAULTS_PROPERTY_SOURCE_NAME = "schedulerLibraryDefaults";

    /**
     * lock-and-fetch claims due executions with FOR UPDATE SKIP LOCKED, so replicas never wait on each other's rows.
     * db-scheduler declares an execution dead after 6 missed heartbeats: 30s means a crashed replica's job is taken
     * over within about 3 minutes instead of the 30-minute default.
     */
    static final Map<String, Object> SCHEDULER_DEFAULT_PROPERTIES = Map.of(
            "db-scheduler.polling-strategy", "lock-and-fetch",
            "db-scheduler.heartbeat-interval", "30s",
            "db-scheduler.delay-startup-until-context-ready", "true");

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            @NonNull SpringApplication springApplication) {

        environment.getPropertySources().addLast(
                new MapPropertySource(SCHEDULER_DEFAULTS_PROPERTY_SOURCE_NAME, SCHEDULER_DEFAULT_PROPERTIES));
    }
}
