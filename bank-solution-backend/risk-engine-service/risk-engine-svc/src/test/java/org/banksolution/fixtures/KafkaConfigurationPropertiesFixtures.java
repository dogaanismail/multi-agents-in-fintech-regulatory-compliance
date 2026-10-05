package org.banksolution.fixtures;

import org.banksolution.config.KafkaConfigurationProperties;

import java.time.Duration;

public final class KafkaConfigurationPropertiesFixtures {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "mock://kafka-consumer-config-tests";
    private static final String CONSUMER_GROUP_ID = "kafka-consumer-config-tests";

    private KafkaConfigurationPropertiesFixtures() {
    }

    public static KafkaConfigurationProperties createKafkaConfigurationProperties(Duration authExceptionRetryInterval) {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.setBootstrapServers(BOOTSTRAP_SERVERS);
        kafkaConfigurationProperties.getSchemaRegistry().setUrl(SCHEMA_REGISTRY_URL);
        kafkaConfigurationProperties.getConsumer().setGroupId(CONSUMER_GROUP_ID);
        kafkaConfigurationProperties.getListener().setAuthExceptionRetryInterval(authExceptionRetryInterval);

        return kafkaConfigurationProperties;
    }
}
