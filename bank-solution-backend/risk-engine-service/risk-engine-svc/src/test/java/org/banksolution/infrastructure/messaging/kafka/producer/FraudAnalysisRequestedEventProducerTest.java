package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.exception.FraudAnalysisRequestedEventException;
import org.banksolution.mapper.CustomerFeaturesMapper;
import org.banksolution.mapper.NetworkFeaturesMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

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
    private KafkaTemplate<String, FraudAnalysisRequestedEvent> fraudAnalysisRequestedEventKafkaTemplate;

    private FraudAnalysisRequestedEventProducer fraudAnalysisRequestedEventProducer;

    @BeforeEach
    void createProducerWithConfiguredTopic() {
        KafkaConfigurationProperties kafkaConfigurationProperties = new KafkaConfigurationProperties();
        kafkaConfigurationProperties.getTopics().getOutgoing().setFraudAnalysisRequested(TOPIC);
        fraudAnalysisRequestedEventProducer = new FraudAnalysisRequestedEventProducer(
                kafkaConfigurationProperties, fraudAnalysisRequestedEventKafkaTemplate);
    }

    @Test
    void shouldSendTheEventToTheConfiguredTopicKeyedByPaymentId() {
        FraudAnalysisRequestedEvent event = createFraudAnalysisRequestedEvent();

        fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(event);

        verify(fraudAnalysisRequestedEventKafkaTemplate).send(TOPIC, PAYMENT_ID, event);
    }

    @Test
    void shouldWrapPublishingFailuresWithThePaymentContext() {
        FraudAnalysisRequestedEvent event = createFraudAnalysisRequestedEvent();
        when(fraudAnalysisRequestedEventKafkaTemplate.send(TOPIC, PAYMENT_ID, event))
                .thenThrow(new IllegalStateException("broker unavailable"));

        assertThatThrownBy(() -> fraudAnalysisRequestedEventProducer.publishFraudAnalysisRequestedEvent(event))
                .isInstanceOf(FraudAnalysisRequestedEventException.class)
                .hasMessageContaining(PAYMENT_ID);
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
