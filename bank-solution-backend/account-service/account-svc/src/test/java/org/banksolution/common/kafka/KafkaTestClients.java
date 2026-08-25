package org.banksolution.common.kafka;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.banksolution.common.initializers.KafkaInitializer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Producers publish into the service's incoming topics; consumers verify what the
 * service published on its outgoing topics (including .DLT parking).
 */
public final class KafkaTestClients {

    private KafkaTestClients() {
    }

    public static <T extends SpecificRecord> KafkaProducer<String, T> createAvroProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaInitializer.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, KafkaInitializer.MOCK_SCHEMA_REGISTRY_URL);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(props);
    }

    /**
     * Fresh random group id per call, reading from the earliest offset, so every test
     * sees the full topic and filters for its own ids instead of depending on order.
     */
    public static KafkaConsumer<String, Object> createAvroConsumer(String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaInitializer.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, KafkaInitializer.MOCK_SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        return consumer;
    }

    public static <T extends SpecificRecord> T awaitMatchingEvent(
            String topic,
            Duration timeout,
            Predicate<T> matcher) {

        try (KafkaConsumer<String, Object> consumer = createAvroConsumer(topic)) {
            long deadline = System.currentTimeMillis() + timeout.toMillis();
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, Object> consumedRecord : consumer.poll(Duration.ofMillis(500))) {
                    @SuppressWarnings("unchecked")
                    T value = (T) consumedRecord.value();
                    if (matcher.test(value)) {
                        return value;
                    }
                }
            }
        }
        throw new AssertionError("No matching event arrived on topic " + topic + " within " + timeout);
    }
}
