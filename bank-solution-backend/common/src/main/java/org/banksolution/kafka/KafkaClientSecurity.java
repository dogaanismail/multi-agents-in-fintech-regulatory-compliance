package org.banksolution.kafka;

import org.springframework.core.env.PropertyResolver;

import java.util.HashMap;
import java.util.Map;

public final class KafkaClientSecurity {

    public static final String SECURITY_PROTOCOL_PROPERTY = "spring.kafka.security.protocol";
    public static final String SASL_MECHANISM_PROPERTY = "spring.kafka.properties.sasl.mechanism";
    public static final String SASL_JAAS_CONFIG_PROPERTY = "spring.kafka.properties.sasl.jaas.config";

    private KafkaClientSecurity() {
    }

    public static Map<String, Object> resolveKafkaClientSecurityProperties(PropertyResolver propertyResolver) {
        Map<String, Object> kafkaClientSecurityProperties = new HashMap<>();
        putIfPresent(kafkaClientSecurityProperties, "security.protocol", propertyResolver.getProperty(SECURITY_PROTOCOL_PROPERTY));
        putIfPresent(kafkaClientSecurityProperties, "sasl.mechanism", propertyResolver.getProperty(SASL_MECHANISM_PROPERTY));
        putIfPresent(kafkaClientSecurityProperties, "sasl.jaas.config", propertyResolver.getProperty(SASL_JAAS_CONFIG_PROPERTY));
        return kafkaClientSecurityProperties;
    }

    private static void putIfPresent(Map<String, Object> kafkaClientSecurityProperties, String kafkaProperty, String value) {
        if (value != null && !value.isBlank()) {
            kafkaClientSecurityProperties.put(kafkaProperty, value);
        }
    }
}
