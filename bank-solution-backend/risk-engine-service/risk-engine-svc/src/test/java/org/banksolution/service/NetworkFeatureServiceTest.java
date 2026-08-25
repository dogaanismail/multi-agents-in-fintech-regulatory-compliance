package org.banksolution.service;

import com.aml.fraud.NetworkFeatures;
import org.banksolution.integration.networktopology.NetworkTopologyServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.IntegrationClientFixtures.createNetworkFeatureResponse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkFeatureServiceTest {

    private static final String ACCOUNT_ID = "account-1";

    @Mock
    private NetworkTopologyServiceClient networkTopologyServiceClient;

    @InjectMocks
    private NetworkFeatureService networkFeatureService;

    @Test
    void shouldMapTheTopologyResponseIntoNetworkFeatures() {
        when(networkTopologyServiceClient.getNetworkFeatures(ACCOUNT_ID))
                .thenReturn(createNetworkFeatureResponse(ACCOUNT_ID));

        NetworkFeatures features = networkFeatureService.getNetworkFeatures(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getInDegree()).isEqualTo(5);
        assertThat(features.getOutDegree()).isEqualTo(8);
    }

    @Test
    void shouldFallBackToZeroedFeaturesWhenTheTopologyLookupFails() {
        when(networkTopologyServiceClient.getNetworkFeatures(ACCOUNT_ID))
                .thenThrow(new IllegalStateException("topology service down"));

        NetworkFeatures features = networkFeatureService.getNetworkFeatures(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getInDegree()).isZero();
        assertThat(features.getPagerank()).isZero();
    }
}
