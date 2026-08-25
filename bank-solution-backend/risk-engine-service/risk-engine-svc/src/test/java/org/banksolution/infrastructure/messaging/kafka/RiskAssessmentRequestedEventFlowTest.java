package org.banksolution.infrastructure.messaging.kafka;

import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.risk.RiskAssessmentRequestedEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.RiskCheckStatus;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.common.initializers.WireMockInitializer.ACCOUNT_SERVICE_BASE_PATH;
import static org.banksolution.common.initializers.WireMockInitializer.CUSTOMER_PROFILE_SERVICE_BASE_PATH;
import static org.banksolution.common.initializers.WireMockInitializer.NETWORK_TOPOLOGY_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.IntegrationClientFixtures.createAccountResponse;
import static org.banksolution.fixtures.IntegrationClientFixtures.createCustomerFeaturesResponse;
import static org.banksolution.fixtures.IntegrationClientFixtures.createNetworkFeatureResponse;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createRiskAssessmentRequestedEvent;

class RiskAssessmentRequestedEventFlowTest extends BaseIntegrationTest {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.kafka.topics.incoming.risk-assessment-requested}")
    private String riskAssessmentRequestedTopic;

    @Value("${spring.kafka.topics.outgoing.fraud-analysis-requested}")
    private String fraudAnalysisRequestedTopic;

    @Autowired
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Test
    void shouldPersistTheRequestAndPublishFraudAnalysisRequestedWithAllFeatureSets()
            throws ExecutionException, InterruptedException {

        String paymentId = "PAY-" + UUID.randomUUID();
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(paymentId);
        stubFeatureEndpoints(event);

        publishRiskAssessmentRequested(event);

        FraudAnalysisRequestedEvent published = KafkaTestClients.awaitMatchingEvent(
                fraudAnalysisRequestedTopic,
                EVENT_TIMEOUT,
                fraudEvent -> paymentId.equals(fraudEvent.getPaymentId()));

        RiskCheckRequestEntity persisted = riskCheckRequestRepository.findAll().stream()
                .filter(entity -> paymentId.equals(entity.getPaymentId()))
                .findFirst()
                .orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(RiskCheckStatus.AWAITING_MARL);
        assertThat(published.getRiskCheckRequestId()).isEqualTo(persisted.getId().toString());
        assertThat(published.getTimestamp()).isEqualTo(event.getTimestamp());
        assertThat(published.getIsCrossBorderPayment()).isTrue();
        assertThat(published.getTransactionFeatures().getSenderAccount()).isEqualTo("GB000111");
        assertThat(published.getTransactionFeatures().getReceiverAccount()).isEqualTo("DE000222");
        assertThat(published.getCustomerFeatures().getTransactionCount()).isEqualTo(42);
        assertThat(published.getNetworkFeatures().getInDegree()).isEqualTo(5);
    }

    @Test
    void shouldFallBackToZeroedCustomerAndNetworkFeaturesWhenThoseServicesAreDown()
            throws ExecutionException, InterruptedException {

        String paymentId = "PAY-" + UUID.randomUUID();
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(paymentId);
        stubAccountBatchLookup(event);

        publishRiskAssessmentRequested(event);

        FraudAnalysisRequestedEvent published = KafkaTestClients.awaitMatchingEvent(
                fraudAnalysisRequestedTopic,
                EVENT_TIMEOUT,
                fraudEvent -> paymentId.equals(fraudEvent.getPaymentId()));

        assertThat(published.getCustomerFeatures().getTransactionCount()).isZero();
        assertThat(published.getNetworkFeatures().getInDegree()).isZero();
    }

    @Test
    void shouldProcessARedeliveredRequestExactlyOnce() throws ExecutionException, InterruptedException {
        String paymentId = "PAY-" + UUID.randomUUID();
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(paymentId);
        stubFeatureEndpoints(event);

        publishRiskAssessmentRequested(event);
        publishRiskAssessmentRequested(event);

        await().atMost(EVENT_TIMEOUT).untilAsserted(
                () -> assertThat(riskCheckRequestRepository.existsByPaymentId(paymentId)).isTrue());
        await().during(Duration.ofSeconds(3)).atMost(EVENT_TIMEOUT).until(
                () -> riskCheckRequestRepository.findAll().stream()
                        .filter(entity -> paymentId.equals(entity.getPaymentId()))
                        .count() == 1);
    }

    private void publishRiskAssessmentRequested(RiskAssessmentRequestedEvent event)
            throws ExecutionException, InterruptedException {

        try (KafkaProducer<String, RiskAssessmentRequestedEvent> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(riskAssessmentRequestedTopic, event.getPaymentId(), event)).get();
        }
    }

    private void stubFeatureEndpoints(RiskAssessmentRequestedEvent event) {
        stubAccountBatchLookup(event);
        stubFor(get(urlPathEqualTo(CUSTOMER_PROFILE_SERVICE_BASE_PATH
                + "/customer/" + event.getCustomerId() + "/features"))
                .willReturn(okJson(objectMapper.writeValueAsString(
                        createCustomerFeaturesResponse(event.getCustomerId(), event.getSourceAccountId())))));
        stubFor(get(urlPathEqualTo(NETWORK_TOPOLOGY_SERVICE_BASE_PATH
                + "/features/" + event.getSourceAccountId()))
                .willReturn(okJson(objectMapper.writeValueAsString(
                        createNetworkFeatureResponse(event.getSourceAccountId())))));
    }

    private void stubAccountBatchLookup(RiskAssessmentRequestedEvent event) {
        stubFor(get(urlPathEqualTo(ACCOUNT_SERVICE_BASE_PATH + "/ids"))
                .willReturn(okJson(objectMapper.writeValueAsString(of(
                        createAccountResponse(UUID.fromString(event.getSourceAccountId()), "GB000111", "GB"),
                        createAccountResponse(UUID.fromString(event.getDestinationAccountId()), "DE000222", "DE"))))));
    }
}
