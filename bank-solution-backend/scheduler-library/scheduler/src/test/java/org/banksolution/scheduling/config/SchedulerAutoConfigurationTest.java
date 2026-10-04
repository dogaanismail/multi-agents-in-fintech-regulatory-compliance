package org.banksolution.scheduling.config;

import com.github.kagkarlsson.scheduler.SchedulerName;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer;
import com.github.kagkarlsson.scheduler.serializer.Serializer;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerAutoConfigurationTest {

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SchedulerAutoConfiguration.class));

    @Test
    void shouldGiveEveryReplicaItsOwnSchedulerInstanceName() {
        applicationContextRunner
                .withPropertyValues("spring.application.name=payment-svc")
                .run(applicationContext -> assertThat(applicationContext.getBean(DbSchedulerCustomizer.class).schedulerName())
                        .map(SchedulerName::getName)
                        .hasValueSatisfying(schedulerInstanceName -> assertThat(schedulerInstanceName).startsWith("payment-svc@")));
    }

    @Test
    void shouldLetAServiceAddASerializerWithoutLosingItsInstanceName() {
        Serializer serviceSerializer = Serializer.DEFAULT_JAVA_SERIALIZER;
        SchedulerInstanceNameCustomizer serviceDbSchedulerCustomizer = new SchedulerInstanceNameCustomizer("payment-engine-svc") {
            @Override
            public Optional<Serializer> serializer() {
                return Optional.of(serviceSerializer);
            }
        };

        assertThat(serviceDbSchedulerCustomizer.schedulerName())
                .map(SchedulerName::getName)
                .hasValueSatisfying(schedulerInstanceName -> assertThat(schedulerInstanceName).startsWith("payment-engine-svc@"));
        assertThat(serviceDbSchedulerCustomizer.serializer()).containsSame(serviceSerializer);
    }

    @Test
    void shouldBackOffWhenTheServiceCustomisesTheSchedulerItself() {
        DbSchedulerCustomizer serviceDbSchedulerCustomizer = new DbSchedulerCustomizer() {
        };

        applicationContextRunner
                .withBean(DbSchedulerCustomizer.class, () -> serviceDbSchedulerCustomizer)
                .run(applicationContext -> assertThat(applicationContext.getBean(DbSchedulerCustomizer.class))
                        .isSameAs(serviceDbSchedulerCustomizer));
    }
}
