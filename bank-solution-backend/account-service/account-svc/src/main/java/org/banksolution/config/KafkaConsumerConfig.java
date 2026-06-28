package org.banksolution.config;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerConfig {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;

    @Bean(name = "kafkaListenerContainerFactory")
    public <T> ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull T> kafkaListenerContainerFactory() {
        return createListenerContainerFactory(avroConsumerFactory());
    }

    @Bean
    public <T> ConsumerFactory<@NonNull String, @NonNull T> avroConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(getCommonConsumerConfigs());
    }

    private Map<String, Object> getCommonConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigurationProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConfigurationProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfigurationProperties.getSchemaRegistry().getUrl());
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        return props;
    }

    private <T> ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull T> createListenerContainerFactory(
            ConsumerFactory<@NonNull String, @NonNull T> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull T> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }

}

