package org.banksolution.integration.customerprofile;

import com.aml.fraud.CustomerFeatures;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.service.CustomerFeatureService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.common.initializers.WireMockInitializer.CUSTOMER_PROFILE_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.IntegrationClientFixtures.createCustomerFeaturesResponse;

class CustomerProfileServiceClientTest extends BaseIntegrationTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ACCOUNT_ID = "account-1";

    @Autowired
    private CustomerFeatureService customerFeatureService;

    @Test
    void shouldDeserializeTheCustomerFeaturesFromTheProfileService() {
        stubFor(get(urlPathEqualTo(customerFeaturesPath()))
                .willReturn(okJson(objectMapper.writeValueAsString(
                        createCustomerFeaturesResponse(CUSTOMER_ID, ACCOUNT_ID)))));

        CustomerFeatures features = customerFeatureService.getCustomerFeatures(CUSTOMER_ID);

        assertThat(features.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(features.getTransactionCount()).isEqualTo(42);
        assertThat(features.getCrossBorderRatio()).isEqualTo(0.25);
    }

    @Test
    void shouldFallBackToZeroedFeaturesWhenTheProfileServiceFails() {
        stubFor(get(urlPathEqualTo(customerFeaturesPath())).willReturn(serverError()));

        CustomerFeatures features = customerFeatureService.getCustomerFeatures(CUSTOMER_ID);

        assertThat(features.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(features.getTransactionCount()).isZero();
        assertThat(features.getTotalAmount()).isZero();
    }

    @Test
    void shouldDeserializeTheCustomerFeaturesLookedUpByAccount() {
        stubFor(get(urlPathEqualTo(CUSTOMER_PROFILE_SERVICE_BASE_PATH + "/account/" + ACCOUNT_ID + "/features"))
                .willReturn(okJson(objectMapper.writeValueAsString(
                        createCustomerFeaturesResponse(CUSTOMER_ID, ACCOUNT_ID)))));

        CustomerFeatures features = customerFeatureService.getCustomerFeaturesByAccountId(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getTransactionCount()).isEqualTo(42);
    }

    private static String customerFeaturesPath() {
        return CUSTOMER_PROFILE_SERVICE_BASE_PATH + "/customer/" + CUSTOMER_ID + "/features";
    }
}
