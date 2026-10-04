package org.banksolution.scheduling.config;

import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;

import java.util.Optional;

public class SchedulerInstanceNameCustomizer implements DbSchedulerCustomizer {

    private final SchedulerName schedulerInstanceName;

    public SchedulerInstanceNameCustomizer(String applicationName) {
        this.schedulerInstanceName = SchedulerInstanceNames.resolveSchedulerInstanceName(applicationName);
    }

    @Override
    public Optional<SchedulerName> schedulerName() {
        return Optional.of(schedulerInstanceName);
    }
}
