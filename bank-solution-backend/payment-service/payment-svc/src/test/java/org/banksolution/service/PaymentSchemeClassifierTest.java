package org.banksolution.service;

import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.exception.UnresolvablePaymentSchemeException;
import org.banksolution.model.request.PaymentRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentSchemeClassifierTest {

    @Test
    void shouldBookAnInternalTransferWhenBothAccountsAreOurs() {
        assertThat(PaymentSchemeClassifier.classify(
                createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.GBP), createPaymentAccounts("GB", "GB")))
                .isEqualTo(PaymentScheme.INTERNAL_TRANSFER);
    }

    @Test
    void shouldRouteBySideWhenTheCounterpartyIsExternal() {
        PaymentRequest outboundRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.GBP);
        outboundRequest.setDestinationAccountId(null);
        PaymentRequest inboundRequest = createDepositRequest(CUSTOMER_ID);

        assertThat(PaymentSchemeClassifier.classify(outboundRequest, null)).isEqualTo(PaymentScheme.EXTERNAL_OUTBOUND);
        assertThat(PaymentSchemeClassifier.classify(inboundRequest, null)).isEqualTo(PaymentScheme.EXTERNAL_INBOUND);
    }

    @Test
    void shouldRefuseARequestThatNamesNoAccountAtAll() {
        PaymentRequest accountlessRequest = createDepositRequest(CUSTOMER_ID);
        accountlessRequest.setDestinationAccountId(null);

        assertThatThrownBy(() -> PaymentSchemeClassifier.classify(accountlessRequest, null))
                .isInstanceOf(UnresolvablePaymentSchemeException.class)
                .hasMessageContaining("DEPOSIT");
    }
}
