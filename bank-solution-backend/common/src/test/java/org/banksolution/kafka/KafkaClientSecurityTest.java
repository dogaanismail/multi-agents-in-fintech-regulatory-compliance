package org.banksolution.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaClientSecurityTest {

    private static final String SCRAM_JAAS_CONFIG =
            "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"risk-engine-service\" password=\"secret\";";

    @Test
    void shouldLeaveTheClientOnPlaintextWhenNoSecurityIsConfigured() {
        assertThat(KafkaClientSecurity.resolveKafkaClientSecurityProperties(new MockEnvironment())).isEmpty();
    }

    @Test
    void shouldPassTheSaslSettingsThroughUnderTheirKafkaClientNames() {
        MockEnvironment mockEnvironment = new MockEnvironment()
                .withProperty(KafkaClientSecurity.SECURITY_PROTOCOL_PROPERTY, "SASL_PLAINTEXT")
                .withProperty(KafkaClientSecurity.SASL_MECHANISM_PROPERTY, "SCRAM-SHA-512")
                .withProperty(KafkaClientSecurity.SASL_JAAS_CONFIG_PROPERTY, SCRAM_JAAS_CONFIG);

        assertThat(KafkaClientSecurity.resolveKafkaClientSecurityProperties(mockEnvironment))
                .containsEntry("security.protocol", "SASL_PLAINTEXT")
                .containsEntry("sasl.mechanism", "SCRAM-SHA-512")
                .containsEntry("sasl.jaas.config", SCRAM_JAAS_CONFIG)
                .hasSize(3);
    }

    @Test
    void shouldIgnoreBlankSettings() {
        MockEnvironment mockEnvironment = new MockEnvironment()
                .withProperty(KafkaClientSecurity.SECURITY_PROTOCOL_PROPERTY, " ");

        assertThat(KafkaClientSecurity.resolveKafkaClientSecurityProperties(mockEnvironment)).isEmpty();
    }
}
