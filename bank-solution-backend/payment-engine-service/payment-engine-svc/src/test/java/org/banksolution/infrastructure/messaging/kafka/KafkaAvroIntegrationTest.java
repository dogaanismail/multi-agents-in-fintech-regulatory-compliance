package org.banksolution.infrastructure.messaging.kafka;

import com.aml.payment.PaymentCreatedEvent;
import com.aml.risk.RiskAssessmentRequestedEvent;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.banksolution.domain.payment.command.InitiatePaymentCommand;
import org.banksolution.fixtures.AvroEventFixtures;
import org.banksolution.fixtures.PaymentFixtures;
import org.banksolution.infrastructure.messaging.kafka.handler.PaymentCreatedEventHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Testcontainers
class KafkaAvroIntegrationTest {

    private static final String SCHEMA_REGISTRY_URL = "mock://payment-engine-tests";

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Test
    void shouldPublishAndConsumeRiskAssessmentRequestedEvent() {
        String topic = "risk.assessment.requested";

        try (KafkaProducer<String, Object> producer = avroProducer()) {
            producer.send(new ProducerRecord<>(topic, PaymentFixtures.PAYMENT_UUID.toString(),
                    AvroEventFixtures.createRiskAssessmentRequestedEvent()));
            producer.flush();
        }

        SpecificRecord consumed = consumeOne(topic);

        assertThat(consumed).isInstanceOf(RiskAssessmentRequestedEvent.class);
        RiskAssessmentRequestedEvent received = (RiskAssessmentRequestedEvent) consumed;
        assertThat(received.getPaymentId()).isEqualTo(PaymentFixtures.PAYMENT_UUID.toString());
        assertThat(received.getAmount()).isEqualTo(PaymentFixtures.AMOUNT.toPlainString());
        assertThat(received.getFromCurrency()).isEqualTo(PaymentFixtures.FROM_CURRENCY);
    }

    @Test
    void shouldConsumePaymentCreatedEventAndDispatchInitiateCommand() {
        String topic = "payment-created-events";

        try (KafkaProducer<String, Object> producer = avroProducer()) {
            producer.send(new ProducerRecord<>(topic, PaymentFixtures.PAYMENT_UUID.toString(),
                    AvroEventFixtures.createPaymentCreatedEvent()));
            producer.flush();
        }

        SpecificRecord consumed = consumeOne(topic);
        assertThat(consumed).isInstanceOf(PaymentCreatedEvent.class);

        CommandGateway commandGateway = mock(CommandGateway.class);
        new PaymentCreatedEventHandler(commandGateway).handle((PaymentCreatedEvent) consumed);

        ArgumentCaptor<InitiatePaymentCommand> captor = ArgumentCaptor.forClass(InitiatePaymentCommand.class);
        verify(commandGateway).send(captor.capture());
        assertThat(captor.getValue().paymentId().getIdentifier()).isEqualTo(PaymentFixtures.PAYMENT_UUID);
    }

    private KafkaProducer<String, Object> avroProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        return new KafkaProducer<>(props, new StringSerializer(), new KafkaAvroSerializer());
    }

    private SpecificRecord consumeOne(String topic) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-engine-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        try (KafkaConsumer<String, Object> consumer =
                     new KafkaConsumer<>(props, new StringDeserializer(), new KafkaAvroDeserializer())) {
            List<TopicPartition> partitions = assignedPartitions(consumer, topic);
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            for (int attempt = 0; attempt < 40; attempt++) {
                ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, Object> record : records) {
                    return (SpecificRecord) record.value();
                }
            }
        }
        throw new AssertionError("No message received from topic " + topic);
    }

    private List<TopicPartition> assignedPartitions(KafkaConsumer<String, Object> consumer, String topic) {
        for (int attempt = 0; attempt < 20; attempt++) {
            List<PartitionInfo> info = consumer.partitionsFor(topic, Duration.ofSeconds(5));
            if (info != null && !info.isEmpty()) {
                List<TopicPartition> partitions = new ArrayList<>();
                for (PartitionInfo partition : info) {
                    partitions.add(new TopicPartition(partition.topic(), partition.partition()));
                }
                return partitions;
            }
        }
        throw new AssertionError("Topic not available: " + topic);
    }
}
