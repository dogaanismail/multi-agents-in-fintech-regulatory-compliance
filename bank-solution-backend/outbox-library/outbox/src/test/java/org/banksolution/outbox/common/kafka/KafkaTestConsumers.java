package org.banksolution.outbox.common.kafka;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.banksolution.outbox.common.initializers.KafkaInitializer;

public final class KafkaTestConsumers {

    private KafkaTestConsumers() {
    }

    public static Optional<ConsumerRecord<String, Object>> awaitMatchingRecord(
            String topic,
            Duration timeout,
            Predicate<ConsumerRecord<String, Object>> recordMatcher) {

        try (KafkaConsumer<String, Object> kafkaConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaInitializer.bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class,
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, KafkaInitializer.MOCK_SCHEMA_REGISTRY_URL))) {

            kafkaConsumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, Object> consumerRecord : kafkaConsumer.poll(Duration.ofMillis(250))) {
                    if (recordMatcher.test(consumerRecord)) {
                        return Optional.of(consumerRecord);
                    }
                }
            }
            return Optional.empty();
        }
    }

    public static String readHeader(ConsumerRecord<String, Object> consumerRecord, String headerName) {
        var header = consumerRecord.headers().lastHeader(headerName);
        return header == null ? null : new String(header.value(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
