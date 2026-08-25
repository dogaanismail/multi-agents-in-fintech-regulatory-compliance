package org.banksolution.common.initializers;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgreSQLInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Started once and shared by every integration test in the JVM, so it deliberately
     * outlives any try-with-resources scope. Testcontainers' Ryuk sidecar removes it
     * when the JVM exits.
     */
    @SuppressWarnings({"resource", "java:S2095"})
    private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.2"));

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {

        if (!POSTGRESQL_CONTAINER.isRunning()) {
            POSTGRESQL_CONTAINER.start();
        }

        TestPropertyValues.of(
                        "spring.datasource.url=" + POSTGRESQL_CONTAINER.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRESQL_CONTAINER.getUsername(),
                        "spring.datasource.password=" + POSTGRESQL_CONTAINER.getPassword())
                .applyTo(configurableApplicationContext.getEnvironment());
    }
}
