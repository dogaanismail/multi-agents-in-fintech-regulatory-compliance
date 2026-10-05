package org.banksolution.outbox.relay;

import io.micrometer.observation.ObservationRegistry;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.banksolution.outbox.entity.OutboxEventEntity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

public class OutboxKafkaSender implements DisposableBean {

    public static final String OUTBOX_EVENT_ID_HEADER = "outbox-event-id";
    public static final String IDEMPOTENCE_KEY_HEADER = "idempotence-key";
    public static final String PAYLOAD_TYPE_HEADER = "payload-type";

    private final DefaultKafkaProducerFactory<String, byte[]> outboxProducerFactory;
    private final KafkaTemplate<String, byte[]> outboxKafkaTemplate;
    private final Duration sendTimeout;

    public OutboxKafkaSender(
            String bootstrapServers,
            Map<String, Object> kafkaClientSecurityProperties,
            Duration sendTimeout,
            ObservationRegistry observationRegistry) {

        this.sendTimeout = sendTimeout;
        this.outboxProducerFactory = new DefaultKafkaProducerFactory<>(
                toOutboxProducerConfig(bootstrapServers, kafkaClientSecurityProperties, sendTimeout));
        this.outboxKafkaTemplate = new KafkaTemplate<>(outboxProducerFactory);

        if (observationRegistry != null) {
            this.outboxKafkaTemplate.setObservationRegistry(observationRegistry);
            this.outboxKafkaTemplate.setObservationEnabled(true);
        }
    }

    public void sendOutboxEventAndAwaitAcknowledgement(
            OutboxEventEntity outboxEventEntity,
            Map<String, String> extraHeaders) {

        ProducerRecord<String, byte[]> producerRecord = toProducerRecord(outboxEventEntity, extraHeaders);
        try {
            outboxKafkaTemplate.send(producerRecord).get(sendTimeout.toMillis() + 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new OutboxDeliveryException("Interrupted while publishing outbox event " + outboxEventEntity.getId(), interruptedException);
        } catch (ExecutionException | TimeoutException | RuntimeException deliveryException) {
            throw new OutboxDeliveryException("Could not publish outbox event " + outboxEventEntity.getId(), deliveryException);
        }
    }

    @Override
    public void destroy() {
        outboxProducerFactory.destroy();
    }

    private static ProducerRecord<String, byte[]> toProducerRecord(
            OutboxEventEntity outboxEventEntity,
            Map<String, String> extraHeaders) {

        ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(
                outboxEventEntity.getDestination(),
                outboxEventEntity.getReferenceId(),
                outboxEventEntity.getPayload());

        addHeader(producerRecord, OUTBOX_EVENT_ID_HEADER, outboxEventEntity.getId().toString());
        addHeader(producerRecord, PAYLOAD_TYPE_HEADER, outboxEventEntity.getPayloadType());
        addHeader(producerRecord, IDEMPOTENCE_KEY_HEADER, outboxEventEntity.getIdempotenceKey());
        extraHeaders.forEach((headerName, headerValue) -> addHeader(producerRecord, headerName, headerValue));

        return producerRecord;
    }

    private static void addHeader(
            ProducerRecord<String, byte[]> producerRecord,
            String headerName,
            String headerValue) {

        if (headerValue != null) {
            producerRecord.headers().add(headerName, headerValue.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static Map<String, Object> toOutboxProducerConfig(
            String bootstrapServers,
            Map<String, Object> kafkaClientSecurityProperties,
            Duration sendTimeout) {

        int sendTimeoutMillis = Math.toIntExact(sendTimeout.toMillis());
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerConfig.putAll(kafkaClientSecurityProperties);
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        producerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
        producerConfig.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerConfig.put(ProducerConfig.CLIENT_ID_CONFIG, "outbox-relay");
        producerConfig.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, sendTimeoutMillis);
        producerConfig.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, Math.max(1000, sendTimeoutMillis / 2));
        producerConfig.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, sendTimeoutMillis);
        producerConfig.put(ProducerConfig.LINGER_MS_CONFIG, 0);

        return producerConfig;
    }
}
