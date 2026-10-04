package org.banksolution.outbox.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private boolean enabled = true;
    private final Kafka kafka = new Kafka();
    private final Relay relay = new Relay();
    private final Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Kafka {
        private String bootstrapServers;
        private String schemaRegistryUrl;
        private Duration sendTimeout = Duration.ofSeconds(10);
    }

    @Getter
    @Setter
    public static class Relay {
        private boolean publishImmediately = true;
        private Duration pollingInterval = Duration.ofSeconds(5);
        private int batchSize = 100;
        private int maxAttempts = 10;
        private Duration initialBackoff = Duration.ofSeconds(1);
        private Duration maxBackoff = Duration.ofMinutes(5);
        private int immediatePublishThreads = 2;
        private int immediatePublishQueueCapacity = 1000;
    }

    @Getter
    @Setter
    public static class Cleanup {
        private Duration retention = Duration.ofDays(7);
        private Duration interval = Duration.ofHours(1);
    }
}
