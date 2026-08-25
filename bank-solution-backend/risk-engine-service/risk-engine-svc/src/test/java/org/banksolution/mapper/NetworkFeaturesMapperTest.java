package org.banksolution.mapper;

import com.aml.fraud.NetworkFeatures;
import org.banksolution.integration.networktopology.dto.NetworkFeatureResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.IntegrationClientFixtures.createNetworkFeatureResponse;

class NetworkFeaturesMapperTest {

    private static final String ACCOUNT_ID = "account-1";

    @Test
    void shouldCopyEveryFeatureFromTheResponse() {
        NetworkFeatureResponse response = createNetworkFeatureResponse(ACCOUNT_ID);

        NetworkFeatures features = NetworkFeaturesMapper.toAvroNetworkFeatures(response);

        assertThat(features.getAccountId()).isEqualTo(response.getAccountId());
        assertThat(features.getInDegree()).isEqualTo(response.getInDegree());
        assertThat(features.getOutDegree()).isEqualTo(response.getOutDegree());
        assertThat(features.getDegreeCentrality()).isEqualTo(response.getDegreeCentrality());
        assertThat(features.getInDegreeCentrality()).isEqualTo(response.getInDegreeCentrality());
        assertThat(features.getOutDegreeCentrality()).isEqualTo(response.getOutDegreeCentrality());
        assertThat(features.getBetweennessCentrality()).isEqualTo(response.getBetweennessCentrality());
        assertThat(features.getClosenessCentrality()).isEqualTo(response.getClosenessCentrality());
        assertThat(features.getPagerank()).isEqualTo(response.getPagerank());
        assertThat(features.getEigenvectorCentrality()).isEqualTo(response.getEigenvectorCentrality());
        assertThat(features.getClusteringCoefficient()).isEqualTo(response.getClusteringCoefficient());
        assertThat(features.getCommunity()).isEqualTo(response.getCommunity());
    }

    @Test
    void shouldZeroEveryFeatureInTheDefaultFallback() {
        NetworkFeatures features = NetworkFeaturesMapper.getDefaultNetworkFeatures(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getInDegree()).isZero();
        assertThat(features.getOutDegree()).isZero();
        assertThat(features.getDegreeCentrality()).isZero();
        assertThat(features.getInDegreeCentrality()).isZero();
        assertThat(features.getOutDegreeCentrality()).isZero();
        assertThat(features.getBetweennessCentrality()).isZero();
        assertThat(features.getClosenessCentrality()).isZero();
        assertThat(features.getPagerank()).isZero();
        assertThat(features.getEigenvectorCentrality()).isZero();
        assertThat(features.getClusteringCoefficient()).isZero();
        assertThat(features.getCommunity()).isZero();
    }
}
