package org.banksolution.config;

import com.aml.payment.PaymentSnapshotEvent;
import com.aml.risk.RiskAssessmentRequestedEvent;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaProducerConfig {

    private final KafkaConfigurationProperties kafkaConfigurationProperties;

    @Bean
    public ProducerFactory<@NonNull String, @NonNull RiskAssessmentRequestedEvent> riskAssessmentProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerProps());
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerProps());
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull RiskAssessmentRequestedEvent> riskAssessmentRequestedEventKafkaTemplate() {
        return new KafkaTemplate<>(riskAssessmentProducerFactory());
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull PaymentSnapshotEvent> paymentSnapshotKafkaTemplate() {
        return new KafkaTemplate<>(paymentSnapshotProducerFactory());
    }

    private Map<String, Object> getCommonProducerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigurationProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaConfigurationProperties.getSchemaRegistry().getUrl());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "payment-engine-service-producer");

        return props;
    }
}
