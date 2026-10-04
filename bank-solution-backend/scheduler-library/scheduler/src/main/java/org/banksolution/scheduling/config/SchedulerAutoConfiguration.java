package org.banksolution.scheduling.config;

import com.github.kagkarlsson.scheduler.boot.autoconfigure.DbSchedulerAutoConfiguration;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = DbSchedulerAutoConfiguration.class)
public class SchedulerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DbSchedulerCustomizer schedulerInstanceNameCustomizer(@Value("${spring.application.name:}") String applicationName) {
        return new SchedulerInstanceNameCustomizer(applicationName);
    }
}
