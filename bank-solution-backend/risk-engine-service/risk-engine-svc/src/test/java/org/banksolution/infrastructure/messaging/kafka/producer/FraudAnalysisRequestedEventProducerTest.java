package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.mapper.CustomerFeaturesMapper;
import org.banksolution.mapper.NetworkFeaturesMapper;
import org.banksolution.outbox.publisher.OutboxEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.TransactionFeaturesFixtures.createTransactionFeatures;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAnalysisRequestedEventProducerTest {

    private static final String TOPIC = "fraud.analysis.requested";
    private static final String PAYMENT_ID = "PAY-1";

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private FraudAnalysisRequestedEventProducer fraudAnalysisRequestedEventProducer;

    @BeforeEach
    void createProducerWithConfiguredTopic() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setFraudAnalysisRequested(TOPIC);
        fraudAnalysisRequestedEventProducer = new FraudAnalysisRequestedEventProducer(kafkaConfigurationProperties, outboxEventPublisher);
    }

    @Test
    void shouldStoreTheEventInTheOutboxKeyedByPaymentAndDeduplicatedByRiskCheck() {
        FraudAnalysisRequestedEvent fraudAnalysisRequestedEvent = createFraudAnalysisRequestedEvent();

        fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(fraudAnalysisRequestedEvent);

        verify(outboxEventPublisher).publish(
                TOPIC,
                PAYMENT_ID,
                fraudAnalysisRequestedEvent,
                "fraud-analysis-requested:" + fraudAnalysisRequestedEvent.getRiskCheckRequestId());
    }

    @Test
    void shouldLetAnOutboxFailureRollBackTheCallersTransaction() {
        FraudAnalysisRequestedEvent fraudAnalysisRequestedEvent = createFraudAnalysisRequestedEvent();
        IllegalStateException outboxFailure = new IllegalStateException("outbox insert failed");
        when(outboxEventPublisher.publish(
                TOPIC,
                PAYMENT_ID,
                fraudAnalysisRequestedEvent,
                "fraud-analysis-requested:" + fraudAnalysisRequestedEvent.getRiskCheckRequestId()))
                .thenThrow(outboxFailure);

        assertThatThrownBy(() -> fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(fraudAnalysisRequestedEvent))
                .isSameAs(outboxFailure);
    }

    private static FraudAnalysisRequestedEvent createFraudAnalysisRequestedEvent() {
        return FraudAnalysisRequestedEvent.newBuilder()
                .setPaymentId(PAYMENT_ID)
                .setRiskCheckRequestId(UUID.randomUUID().toString())
                .setTimestamp(1755000000000L)
                .setIsCrossBorderPayment(false)
                .setTransactionFeatures(createTransactionFeatures(PAYMENT_ID, "GB", "GB"))
                .setCustomerFeatures(CustomerFeaturesMapper.getDefaultCustomerFeatures("customer-1", ""))
                .setNetworkFeatures(NetworkFeaturesMapper.getDefaultNetworkFeatures("account-1"))
                .build();
    }
}
