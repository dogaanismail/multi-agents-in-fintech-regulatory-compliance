package org.banksolution.config;

import lombok.RequiredArgsConstructor;
import org.axonframework.extensions.kafka.eventhandling.producer.ConfirmationMode;
import org.axonframework.extensions.kafka.eventhandling.producer.DefaultProducerFactory;
import org.axonframework.extensions.kafka.eventhandling.producer.ProducerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class AxonKafkaConfig {

    private final KafkaConfigurationProperties  kafkaConfigurationProperties;

    @Bean
    public ProducerFactory<String, Object> axonKafkaProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", kafkaConfigurationProperties.getBootstrapServers());
        config.put("schema.registry.url", kafkaConfigurationProperties.getSchemaRegistry().getUrl());
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer");
        config.put("acks", "all");
        config.put("retries", 3);
        config.put("enable.idempotence", true);
        config.put("client.id", "payment-engine-axon-producer");

        return DefaultProducerFactory.<String, Object>builder()
                .configuration(config)
                .confirmationMode(ConfirmationMode.TRANSACTIONAL)
                .transactionalIdPrefix("payment-engine-tx-")
                .build();
    }
}
