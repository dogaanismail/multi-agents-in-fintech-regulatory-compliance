package org.banksolution.util;

import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentType;
import org.banksolution.model.request.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentRequestUtilTest {

    @ParameterizedTest
    @EnumSource(value = PaymentType.class, names = {"TRANSFER_OUT", "WITHDRAWAL"})
    void shouldRequireASourceAccountForDebits(PaymentType paymentType) {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.GBP);
        paymentRequest.setPaymentType(paymentType);
        assertThatCode(() -> PaymentRequestUtil.validatePaymentRequest(paymentRequest)).doesNotThrowAnyException();

        paymentRequest.setSourceAccountId(null);

        assertThatThrownBy(() -> PaymentRequestUtil.validatePaymentRequest(paymentRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source account is required for " + paymentType);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentType.class, names = {"TRANSFER_IN", "DEPOSIT"})
    void shouldRequireADestinationAccountForCredits(PaymentType paymentType) {
        PaymentRequest paymentRequest = createDepositRequest(CUSTOMER_ID);
        paymentRequest.setPaymentType(paymentType);
        assertThatCode(() -> PaymentRequestUtil.validatePaymentRequest(paymentRequest)).doesNotThrowAnyException();

        paymentRequest.setDestinationAccountId(null);

        assertThatThrownBy(() -> PaymentRequestUtil.validatePaymentRequest(paymentRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Destination account is required for " + paymentType);
    }
}
