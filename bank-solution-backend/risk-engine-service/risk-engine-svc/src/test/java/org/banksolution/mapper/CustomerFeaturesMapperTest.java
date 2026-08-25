package org.banksolution.mapper;

import com.aml.fraud.CustomerFeatures;
import org.banksolution.integration.customerprofile.dto.CustomerFeaturesResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.IntegrationClientFixtures.createCustomerFeaturesResponse;

class CustomerFeaturesMapperTest {

    private static final String CUSTOMER_ID = "customer-1";
    private static final String ACCOUNT_ID = "account-1";

    @Test
    void shouldCopyEveryFeatureFromTheResponse() {
        CustomerFeaturesResponse response = createCustomerFeaturesResponse(CUSTOMER_ID, ACCOUNT_ID);

        CustomerFeatures features = CustomerFeaturesMapper.toAvroCustomerFeatures(response);

        assertThat(features.getCustomerId()).isEqualTo(response.getCustomerId());
        assertThat(features.getAccountId()).isEqualTo(response.getAccountId());
        assertThat(features.getTransactionCount()).isEqualTo(response.getTransactionCount());
        assertThat(features.getTotalAmount()).isEqualTo(response.getTotalAmount());
        assertThat(features.getAvgAmount()).isEqualTo(response.getAvgAmount());
        assertThat(features.getMedianAmount()).isEqualTo(response.getMedianAmount());
        assertThat(features.getMaxAmount()).isEqualTo(response.getMaxAmount());
        assertThat(features.getMinAmount()).isEqualTo(response.getMinAmount());
        assertThat(features.getStdAmount()).isEqualTo(response.getStdAmount());
        assertThat(features.getActiveDays()).isEqualTo(response.getActiveDays());
        assertThat(features.getTransactionsPerDay()).isEqualTo(response.getTransactionsPerDay());
        assertThat(features.getCrossBorderRatio()).isEqualTo(response.getCrossBorderRatio());
        assertThat(features.getCashTransactionRatio()).isEqualTo(response.getCashTransactionRatio());
        assertThat(features.getLargeTransactionRatio()).isEqualTo(response.getLargeTransactionRatio());
        assertThat(features.getNightTransactionRatio()).isEqualTo(response.getNightTransactionRatio());
        assertThat(features.getWeekendTransactionRatio()).isEqualTo(response.getWeekendTransactionRatio());
        assertThat(features.getUniqueReceivers()).isEqualTo(response.getUniqueReceivers());
        assertThat(features.getUniqueReceiverCountries()).isEqualTo(response.getUniqueReceiverCountries());
        assertThat(features.getReceiverDiversity()).isEqualTo(response.getReceiverDiversity());
        assertThat(features.getUniqueCurrencies()).isEqualTo(response.getUniqueCurrencies());
        assertThat(features.getAmountConsistency()).isEqualTo(response.getAmountConsistency());
    }

    @Test
    void shouldMapANullAccountIdToAnEmptyString() {
        CustomerFeaturesResponse response = createCustomerFeaturesResponse(CUSTOMER_ID, null);

        CustomerFeatures features = CustomerFeaturesMapper.toAvroCustomerFeatures(response);

        assertThat(features.getAccountId()).isEmpty();
    }

    @Test
    void shouldZeroEveryFeatureInTheDefaultFallback() {
        CustomerFeatures features = CustomerFeaturesMapper.getDefaultCustomerFeatures(CUSTOMER_ID, ACCOUNT_ID);

        assertThat(features.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getTransactionCount()).isZero();
        assertThat(features.getTotalAmount()).isZero();
        assertThat(features.getAvgAmount()).isZero();
        assertThat(features.getMedianAmount()).isZero();
        assertThat(features.getMaxAmount()).isZero();
        assertThat(features.getMinAmount()).isZero();
        assertThat(features.getStdAmount()).isZero();
        assertThat(features.getActiveDays()).isZero();
        assertThat(features.getTransactionsPerDay()).isZero();
        assertThat(features.getCrossBorderRatio()).isZero();
        assertThat(features.getCashTransactionRatio()).isZero();
        assertThat(features.getLargeTransactionRatio()).isZero();
        assertThat(features.getNightTransactionRatio()).isZero();
        assertThat(features.getWeekendTransactionRatio()).isZero();
        assertThat(features.getUniqueReceivers()).isZero();
        assertThat(features.getUniqueReceiverCountries()).isZero();
        assertThat(features.getReceiverDiversity()).isZero();
        assertThat(features.getUniqueCurrencies()).isZero();
        assertThat(features.getAmountConsistency()).isZero();
    }
}
