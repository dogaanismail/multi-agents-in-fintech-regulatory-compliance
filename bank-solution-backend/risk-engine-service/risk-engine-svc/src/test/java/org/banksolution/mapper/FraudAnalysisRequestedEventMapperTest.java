package org.banksolution.mapper;

import com.aml.fraud.CustomerFeatures;
import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.fraud.NetworkFeatures;
import com.aml.fraud.TransactionFeatures;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.TransactionFeaturesFixtures.createTransactionFeatures;

class FraudAnalysisRequestedEventMapperTest {

    private static final String PAYMENT_ID = "PAY-1";
    private static final String RISK_CHECK_REQUEST_ID = UUID.randomUUID().toString();
    private static final long TIMESTAMP = 1755000000000L;

    private final CustomerFeatures customerFeatures =
            CustomerFeaturesMapper.getDefaultCustomerFeatures("customer-1", "account-1");
    private final NetworkFeatures networkFeatures =
            NetworkFeaturesMapper.getDefaultNetworkFeatures("account-1");

    @Test
    void shouldAssembleTheEventAroundTheThreeFeatureSets() {
        TransactionFeatures transactionFeatures = createTransactionFeatures(PAYMENT_ID, "GB", "GB");

        FraudAnalysisRequestedEvent event = FraudAnalysisRequestedEventMapper.toAvroRequest(
                RISK_CHECK_REQUEST_ID, TIMESTAMP, transactionFeatures, customerFeatures, networkFeatures);

        assertThat(event.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.getRiskCheckRequestId()).isEqualTo(RISK_CHECK_REQUEST_ID);
        assertThat(event.getTimestamp()).isEqualTo(TIMESTAMP);
        assertThat(event.getTransactionFeatures()).isEqualTo(transactionFeatures);
        assertThat(event.getCustomerFeatures()).isEqualTo(customerFeatures);
        assertThat(event.getNetworkFeatures()).isEqualTo(networkFeatures);
    }

    @Test
    void shouldFlagTheEventAsCrossBorderWhenSenderAndReceiverBanksDiffer() {
        TransactionFeatures transactionFeatures = createTransactionFeatures(PAYMENT_ID, "GB", "DE");

        FraudAnalysisRequestedEvent event = FraudAnalysisRequestedEventMapper.toAvroRequest(
                RISK_CHECK_REQUEST_ID, TIMESTAMP, transactionFeatures, customerFeatures, networkFeatures);

        assertThat(event.getIsCrossBorderPayment()).isTrue();
    }

    @Test
    void shouldNotFlagTheEventAsCrossBorderWhenBothBanksShareALocation() {
        TransactionFeatures transactionFeatures = createTransactionFeatures(PAYMENT_ID, "GB", "GB");

        FraudAnalysisRequestedEvent event = FraudAnalysisRequestedEventMapper.toAvroRequest(
                RISK_CHECK_REQUEST_ID, TIMESTAMP, transactionFeatures, customerFeatures, networkFeatures);

        assertThat(event.getIsCrossBorderPayment()).isFalse();
    }

    @Test
    void shouldCompareBankLocationsCaseInsensitively() {
        assertThat(FraudAnalysisRequestedEventMapper.isCrossBorder("gb", "GB")).isFalse();
        assertThat(FraudAnalysisRequestedEventMapper.isCrossBorder("GB", "DE")).isTrue();
    }

    @Test
    void shouldTreatAMissingLocationAsNotCrossBorder() {
        assertThat(FraudAnalysisRequestedEventMapper.isCrossBorder(null, "GB")).isFalse();
        assertThat(FraudAnalysisRequestedEventMapper.isCrossBorder("GB", null)).isFalse();
        assertThat(FraudAnalysisRequestedEventMapper.isCrossBorder(null, null)).isFalse();
    }
}
