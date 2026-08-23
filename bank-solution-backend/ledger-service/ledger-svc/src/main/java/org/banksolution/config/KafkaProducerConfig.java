package org.banksolution.config;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.WalletBalanceChangedEvent;
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

    private static final int RETRIES = 3;

    private final KafkaConfigurationProperties kafkaConfigurationProperties;

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull LedgerPostingCompletedEvent> ledgerPostingCompletedEventKafkaTemplate() {

        var template = new KafkaTemplate<>(ledgerPostingCompletedEventProducerFactory());
        template.setObservationEnabled(true);

        return template;
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull LedgerPostingCompletedEvent>
    ledgerPostingCompletedEventProducerFactory() {

        return new DefaultKafkaProducerFactory<>(getProducerConfigs());
    }

    @Bean
    public KafkaTemplate<@NonNull String, @NonNull WalletBalanceChangedEvent> walletBalanceChangedEventKafkaTemplate() {
        var template = new KafkaTemplate<>(walletBalanceChangedEventProducerFactory());
        template.setObservationEnabled(true);

        return template;
    }

    @Bean
    public ProducerFactory<@NonNull String, @NonNull WalletBalanceChangedEvent> walletBalanceChangedEventProducerFactory() {

        return new DefaultKafkaProducerFactory<>(getProducerConfigs());
    }

    private Map<String, Object> getProducerConfigs() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigurationProperties.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                kafkaConfigurationProperties.getSchemaRegistry().getUrl());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, RETRIES);

        return props;
    }
}
