package org.banksolution.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaConfigurationProperties {

    private String bootstrapServers;
    private SchemaRegistry schemaRegistry = new SchemaRegistry();
    private Consumer consumer = new Consumer();
    private Topics topics = new Topics();

    @Data
    public static class SchemaRegistry {
        private String url;
    }

    @Data
    public static class Consumer {
        private String groupId;
    }

    @Data
    public static class Topics {
        private Incoming incoming = new Incoming();
        private Outgoing outgoing = new Outgoing();

        @Data
        public static class Incoming {
            private String riskAssessmentRequested;
            private String fraudAnalysisCompleted;
        }

        @Data
        public static class Outgoing {
            private String riskAssessmentCompleted;
            private String fraudAnalysisRequested;
        }
    }
}
