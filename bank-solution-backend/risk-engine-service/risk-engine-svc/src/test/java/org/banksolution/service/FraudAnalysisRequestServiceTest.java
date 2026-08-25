package org.banksolution.service;

import com.aml.fraud.CustomerFeatures;
import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.fraud.NetworkFeatures;
import com.aml.fraud.TransactionFeatures;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.infrastructure.messaging.kafka.producer.FraudAnalysisRequestedEventProducer;
import org.banksolution.mapper.CustomerFeaturesMapper;
import org.banksolution.mapper.NetworkFeaturesMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.banksolution.fixtures.TransactionFeaturesFixtures.createTransactionFeatures;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAnalysisRequestServiceTest {

    @Mock
    private TransactionFeatureService transactionFeatureService;

    @Mock
    private CustomerFeatureService customerFeatureService;

    @Mock
    private NetworkFeatureService networkFeatureService;

    @Mock
    private FraudAnalysisRequestedEventProducer fraudAnalysisRequestedEventProducer;

    @InjectMocks
    private FraudAnalysisRequestService fraudAnalysisRequestService;

    @Test
    void shouldGatherAllThreeFeatureSetsAndPublishTheAssembledEvent() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        TransactionFeatures transactionFeatures =
                createTransactionFeatures(riskCheckRequest.getPaymentId(), "GB", "DE");
        CustomerFeatures customerFeatures = CustomerFeaturesMapper
                .getDefaultCustomerFeatures(riskCheckRequest.getCustomerId(), "");
        NetworkFeatures networkFeatures = NetworkFeaturesMapper
                .getDefaultNetworkFeatures(riskCheckRequest.getSourceAccountId());

        when(transactionFeatureService.getTransactionFeatures(riskCheckRequest)).thenReturn(transactionFeatures);
        when(customerFeatureService.getCustomerFeatures(riskCheckRequest.getCustomerId()))
                .thenReturn(customerFeatures);
        when(networkFeatureService.getNetworkFeatures(riskCheckRequest.getSourceAccountId()))
                .thenReturn(networkFeatures);

        fraudAnalysisRequestService.processFraudAnalysisRequest(riskCheckRequest);

        ArgumentCaptor<FraudAnalysisRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(FraudAnalysisRequestedEvent.class);
        verify(fraudAnalysisRequestedEventProducer).publishFraudAnalysisRequestedEvent(eventCaptor.capture());

        FraudAnalysisRequestedEvent event = eventCaptor.getValue();
        assertThat(event.getPaymentId()).isEqualTo(riskCheckRequest.getPaymentId());
        assertThat(event.getRiskCheckRequestId()).isEqualTo(riskCheckRequest.getId().toString());
        assertThat(event.getTimestamp()).isEqualTo(riskCheckRequest.getRequestTimestamp());
        assertThat(event.getIsCrossBorderPayment()).isTrue();
        assertThat(event.getTransactionFeatures()).isEqualTo(transactionFeatures);
        assertThat(event.getCustomerFeatures()).isEqualTo(customerFeatures);
        assertThat(event.getNetworkFeatures()).isEqualTo(networkFeatures);
    }
}
