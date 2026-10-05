package org.banksolution.outbox.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxAutoConfigurationTest {

    @Test
    void shouldRelayOverPlaintextWhenNoKafkaSecurityIsConfigured() {
        assertThat(OutboxAutoConfiguration.resolveKafkaClientSecurityProperties(new MockEnvironment())).isEmpty();
    }

    @Test
    void shouldAuthenticateTheRelayWithTheServicesKafkaCredentials() {
        String scramJaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"payment-service\" password=\"secret\";";
        MockEnvironment mockEnvironment = new MockEnvironment()
                .withProperty("spring.kafka.security.protocol", "SASL_PLAINTEXT")
                .withProperty("spring.kafka.properties.sasl.mechanism", "SCRAM-SHA-512")
                .withProperty("spring.kafka.properties.sasl.jaas.config", scramJaasConfig);

        assertThat(OutboxAutoConfiguration.resolveKafkaClientSecurityProperties(mockEnvironment))
                .containsEntry("security.protocol", "SASL_PLAINTEXT")
                .containsEntry("sasl.mechanism", "SCRAM-SHA-512")
                .containsEntry("sasl.jaas.config", scramJaasConfig)
                .hasSize(3);
    }
}
