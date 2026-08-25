package org.banksolution.integration.networktopology;

import com.aml.fraud.NetworkFeatures;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.service.NetworkFeatureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.common.initializers.WireMockInitializer.NETWORK_TOPOLOGY_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.IntegrationClientFixtures.createNetworkFeatureResponse;

class NetworkTopologyServiceClientTest extends BaseIntegrationTest {

    private static final String ACCOUNT_ID = "account-1";

    @Autowired
    private NetworkFeatureService networkFeatureService;

    @Test
    void shouldDeserializeTheNetworkFeaturesFromTheTopologyService() {
        stubFor(get(urlPathEqualTo(networkFeaturesPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(createNetworkFeatureResponse(ACCOUNT_ID)))));

        NetworkFeatures features = networkFeatureService.getNetworkFeatures(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getInDegree()).isEqualTo(5);
        assertThat(features.getPagerank()).isEqualTo(0.0009);
    }

    @Test
    void shouldFallBackToZeroedFeaturesWhenTheAccountIsUnknownToTheTopology() {
        stubFor(get(urlPathEqualTo(networkFeaturesPath())).willReturn(notFound()));

        NetworkFeatures features = networkFeatureService.getNetworkFeatures(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getInDegree()).isZero();
        assertThat(features.getPagerank()).isZero();
    }

    private static String networkFeaturesPath() {
        return NETWORK_TOPOLOGY_SERVICE_BASE_PATH + "/features/" + ACCOUNT_ID;
    }
}
