package org.banksolution.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.KafkaConfigurationPropertiesFixtures.createKafkaConfigurationProperties;

class KafkaConsumerConfigTest {

    private static final Duration AUTH_EXCEPTION_RETRY_INTERVAL = Duration.ofSeconds(10);

    @Test
    void shouldKeepRetryingAuthenticationFailuresInsteadOfStoppingTheListenerContainer() {
        KafkaConfigurationProperties kafkaConfigurationProperties =
                createKafkaConfigurationProperties(AUTH_EXCEPTION_RETRY_INTERVAL);
        KafkaConsumerConfig kafkaConsumerConfig =
                new KafkaConsumerConfig(kafkaConfigurationProperties, new SimpleMeterRegistry(), new MockEnvironment());

        ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory =
                kafkaConsumerConfig.kafkaListenerContainerFactory();

        assertThat(kafkaListenerContainerFactory.getContainerProperties().getAuthExceptionRetryInterval())
                .isEqualTo(AUTH_EXCEPTION_RETRY_INTERVAL);
    }
}
