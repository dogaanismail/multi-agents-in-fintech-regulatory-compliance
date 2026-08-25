package org.banksolution.common.initializers;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * The mock:// scheme resolves to an in-JVM schema registry shared by every serializer
     * and deserializer using the same scope, so no Schema Registry container is needed.
     */
    public static final String MOCK_SCHEMA_REGISTRY_URL = "mock://payment-engine-tests";

    /**
     * Started once and shared by every integration test in the JVM, so it deliberately
     * outlives any try-with-resources scope. Testcontainers' Ryuk sidecar removes it
     * when the JVM exits.
     */
    @SuppressWarnings({"resource", "java:S2095"})
    private static final ConfluentKafkaContainer KAFKA_CONTAINER =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext configurableApplicationContext) {

        if (!KAFKA_CONTAINER.isRunning()) {
            KAFKA_CONTAINER.start();
        }

        // The Axon Kafka extension publishes the domain-event stream through its own producer,
        // so it needs the broker address as well.
        TestPropertyValues.of(
                        "spring.kafka.bootstrap-servers=" + KAFKA_CONTAINER.getBootstrapServers(),
                        "spring.kafka.schema-registry.url=" + MOCK_SCHEMA_REGISTRY_URL,
                        "axon.kafka.bootstrap-servers=" + KAFKA_CONTAINER.getBootstrapServers(),
                        "axon.kafka.properties.schema.registry.url=" + MOCK_SCHEMA_REGISTRY_URL)
                .applyTo(configurableApplicationContext.getEnvironment());
    }

    public static String bootstrapServers() {
        return KAFKA_CONTAINER.getBootstrapServers();
    }
}
