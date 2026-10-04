package org.banksolution.outbox.serialization;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.util.Map;

import org.apache.avro.specific.SpecificRecord;

public class OutboxPayloadSerializer {

    private final KafkaAvroSerializer kafkaAvroSerializer;

    public OutboxPayloadSerializer(String schemaRegistryUrl) {
        this.kafkaAvroSerializer = new KafkaAvroSerializer();
        this.kafkaAvroSerializer.configure(
                Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl),
                false);
    }

    public byte[] serializeOutboxPayload(
            String destination,
            SpecificRecord payload) {

        return kafkaAvroSerializer.serialize(destination, payload);
    }
}
