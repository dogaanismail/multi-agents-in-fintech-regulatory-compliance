package org.banksolution.scheduling.integration;

import com.github.kagkarlsson.scheduler.Scheduler;

import java.time.Duration;
import java.util.List;

import org.banksolution.scheduling.SchedulerTestApplication;
import org.banksolution.scheduling.probe.SingleExecutionProbe;
import org.banksolution.scheduling.probe.SingleExecutionProbe.ProbeExecution;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Tag("integration")
class SchedulerSingleExecutionIntegrationTest {

    private static final String FIRST_REPLICA_NAME = "probe-replica-a";
    private static final String SECOND_REPLICA_NAME = "probe-replica-b";
    private static final int EXPECTED_PROBE_EXECUTIONS = 20;

    /**
     * Started once for the JVM; Testcontainers' Ryuk sidecar removes it when the JVM exits.
     */
    @SuppressWarnings({"resource", "java:S2095"})
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2"));

    @Test
    void shouldRunEveryScheduledExecutionOnExactlyOneOfTwoReplicas() {
        POSTGRESQL_CONTAINER.start();

        try (ConfigurableApplicationContext firstReplica = startReplica(FIRST_REPLICA_NAME);
             ConfigurableApplicationContext secondReplica = startReplica(SECOND_REPLICA_NAME)) {

            assertThat(firstReplica.getBean(Scheduler.class).getSchedulerState().isStarted()).isTrue();
            assertThat(secondReplica.getBean(Scheduler.class).getSchedulerState().isStarted()).isTrue();
            await().atMost(Duration.ofSeconds(60))
                    .until(() -> SingleExecutionProbe.findProbeExecutions().size() >= EXPECTED_PROBE_EXECUTIONS);
        }

        List<ProbeExecution> probeExecutions = SingleExecutionProbe.findProbeExecutions();
        assertThat(probeExecutions).extracting(ProbeExecution::executionTime).doesNotHaveDuplicates();
        assertThat(SingleExecutionProbe.findMaxConcurrentExecutions()).isEqualTo(1);
        assertThat(probeExecutions)
                .extracting(probeExecution -> probeExecution.pickedBy().substring(0, probeExecution.pickedBy().indexOf('@')))
                .containsOnly(FIRST_REPLICA_NAME, SECOND_REPLICA_NAME)
                .contains(FIRST_REPLICA_NAME, SECOND_REPLICA_NAME);
    }

    private static ConfigurableApplicationContext startReplica(String replicaName) {
        return new SpringApplicationBuilder(SchedulerTestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.application.name=" + replicaName,
                        "spring.datasource.url=" + POSTGRESQL_CONTAINER.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRESQL_CONTAINER.getUsername(),
                        "spring.datasource.password=" + POSTGRESQL_CONTAINER.getPassword(),
                        "spring.liquibase.change-log=classpath:scheduler/db.changelog-scheduler.xml",
                        "db-scheduler.polling-interval=100ms")
                .run();
    }
}
