package org.banksolution.config;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;
import com.github.kagkarlsson.scheduler.task.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Configuration
@Slf4j
public class SchedulerConfig {

    @Bean
    public Scheduler scheduler(
            DataSource dataSource,
            List<Task<?>> knownTasks) {

        return Scheduler.create(dataSource, knownTasks)
                .serializer(new JacksonSerializer())
                .build();
    }

    @Bean
    DbSchedulerCustomizer customizer() {
        return new DbSchedulerCustomizer() {
            @Override
            public Optional<SchedulerName> schedulerName() {
                return Optional.of(new SchedulerName.Fixed("spring-boot-scheduler-1"));
            }

            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(new JacksonSerializer());
            }
        };
    }

}
