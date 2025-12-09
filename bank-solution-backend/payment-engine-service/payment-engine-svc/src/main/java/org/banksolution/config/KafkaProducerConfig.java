package org.banksolution.config;

import com.aml.payment.PaymentSnapshotEvent;
import com.aml.risk.RiskCheckRequest;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer Configuration - Pure Snapshot Architecture
 * <p>
 * Only two producers needed:
 * 1. RiskCheckRequest - outgoing risk check requests to risk-engine
 * 2. PaymentSnapshotEvent - snapshots to payment-history service
 * <p>
 * Individual event publishers (PaymentCompleted, PaymentBlocked) removed
 * as snapshots contain all state changes.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry.url}")
    private String schemaRegistryUrl;

    private Map<String, Object> getCommonProducerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "payment-engine-service-producer");
        return props;
    }

    @Bean
    public ProducerFactory<String, RiskCheckRequest> riskCheckProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerProps());
    }

    @Bean
    public ProducerFactory<String, PaymentSnapshotEvent> paymentSnapshotProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerProps());
    }

    @Bean
    public KafkaTemplate<String, RiskCheckRequest> riskCheckKafkaTemplate() {
        return new KafkaTemplate<>(riskCheckProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, PaymentSnapshotEvent> paymentSnapshotKafkaTemplate() {
        return new KafkaTemplate<>(paymentSnapshotProducerFactory());
    }
}
