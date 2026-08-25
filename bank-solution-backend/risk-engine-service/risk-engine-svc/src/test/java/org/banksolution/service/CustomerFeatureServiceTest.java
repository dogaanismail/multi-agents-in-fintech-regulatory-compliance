package org.banksolution.service;

import com.aml.fraud.CustomerFeatures;
import org.banksolution.integration.customerprofile.CustomerProfileServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.IntegrationClientFixtures.createCustomerFeaturesResponse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerFeatureServiceTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ACCOUNT_ID = "account-1";

    @Mock
    private CustomerProfileServiceClient customerProfileServiceClient;

    @InjectMocks
    private CustomerFeatureService customerFeatureService;

    @Test
    void shouldMapTheProfileResponseWhenLookingUpByCustomer() {
        when(customerProfileServiceClient.getCustomerFeatures(CUSTOMER_ID))
                .thenReturn(createCustomerFeaturesResponse(CUSTOMER_ID, ACCOUNT_ID));

        CustomerFeatures features = customerFeatureService.getCustomerFeatures(CUSTOMER_ID);

        assertThat(features.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(features.getTransactionCount()).isEqualTo(42);
    }

    @Test
    void shouldFallBackToZeroedFeaturesWhenTheCustomerLookupFails() {
        when(customerProfileServiceClient.getCustomerFeatures(CUSTOMER_ID))
                .thenThrow(new IllegalStateException("profile service down"));

        CustomerFeatures features = customerFeatureService.getCustomerFeatures(CUSTOMER_ID);

        assertThat(features.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(features.getAccountId()).isEmpty();
        assertThat(features.getTransactionCount()).isZero();
    }

    @Test
    void shouldMapTheProfileResponseWhenLookingUpByAccount() {
        when(customerProfileServiceClient.getFeaturesByAccountId(ACCOUNT_ID))
                .thenReturn(createCustomerFeaturesResponse(CUSTOMER_ID, ACCOUNT_ID));

        CustomerFeatures features = customerFeatureService.getCustomerFeaturesByAccountId(ACCOUNT_ID);

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getTransactionCount()).isEqualTo(42);
    }

    @Test
    void shouldFallBackToZeroedFeaturesWhenTheAccountLookupFails() {
        when(customerProfileServiceClient.getFeaturesByAccountId(ACCOUNT_ID))
                .thenThrow(new IllegalStateException("profile service down"));

        CustomerFeatures features = customerFeatureService.getCustomerFeaturesByAccountId(ACCOUNT_ID);

        assertThat(features.getCustomerId()).isEmpty();
        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getTransactionCount()).isZero();
    }
}
