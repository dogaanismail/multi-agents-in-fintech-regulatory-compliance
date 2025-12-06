package org.banksolution.config;

import com.aml.fraud.FraudDetectionRequest;
import com.aml.payment.PaymentBlockedEvent;
import com.aml.payment.PaymentCompletedEvent;
import com.aml.risk.RiskCheckRequest;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.NonNull;
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

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull RiskCheckRequest> riskCheckKafkaTemplate() {
        return new KafkaTemplate<>(riskCheckRequestProducerFactory());
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull FraudDetectionRequest> fraudDetectionRequestKafkaTemplate() {
        return new KafkaTemplate<>(fraudDetectionRequestProducerFactory());
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull PaymentCompletedEvent> paymentCompletedEventKafkaTemplate() {
        return new KafkaTemplate<>(paymentCompletedEventProducerFactory());
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull PaymentBlockedEvent> paymentBlockedEventKafkaTemplate() {
        return new KafkaTemplate<>(paymentBlockedEventProducerFactory());
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull RiskCheckRequest> riskCheckRequestProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerConfigs());
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull FraudDetectionRequest> fraudDetectionRequestProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerConfigs());
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull PaymentCompletedEvent> paymentCompletedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerConfigs());
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull PaymentBlockedEvent> paymentBlockedEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(getCommonProducerConfigs());
    }

    private Map<String, Object> getCommonProducerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return props;
    }
}
