package org.banksolution.mapper;

import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentType;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentRequestMapperTest {

    @Test
    void shouldCopyTheRequestedFieldsAndLeaveTheDerivedOnesForTheService() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);

        PaymentRequestEntity paymentRequestEntity = PaymentRequestMapper.toPaymentRequestEntity(paymentRequest);

        assertThat(paymentRequestEntity.getId()).isNull();
        assertThat(paymentRequestEntity.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(paymentRequestEntity.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(paymentRequestEntity.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(paymentRequestEntity.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(paymentRequestEntity.getFromCurrency()).isEqualTo(Currency.GBP);
        assertThat(paymentRequestEntity.getToCurrency()).isEqualTo(Currency.EUR);
        assertThat(paymentRequestEntity.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(paymentRequestEntity.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(paymentRequestEntity.getConvertedAmount()).isNull();
        assertThat(paymentRequestEntity.getAppliedExchangeRate()).isNull();
        assertThat(paymentRequestEntity.getPaymentScheme()).isNull();
        assertThat(paymentRequestEntity.getFixedSide()).isNull();
    }

    @Test
    void shouldCopyEveryPersistedFieldIntoTheResponseWithTheGivenMessage() {
        UUID paymentId = UUID.randomUUID();
        PaymentRequestEntity paymentRequestEntity = createPersistedPaymentRequestEntity(paymentId, CUSTOMER_ID);

        PaymentRequestResponse paymentRequestResponse =
                PaymentRequestMapper.toPaymentRequestResponse(paymentRequestEntity, "submitted");

        assertThat(paymentRequestResponse.getId()).isEqualTo(paymentId);
        assertThat(paymentRequestResponse.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(paymentRequestResponse.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID);
        assertThat(paymentRequestResponse.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID);
        assertThat(paymentRequestResponse.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(paymentRequestResponse.getFromCurrency()).isEqualTo(Currency.GBP);
        assertThat(paymentRequestResponse.getToCurrency()).isEqualTo(Currency.EUR);
        assertThat(paymentRequestResponse.getConvertedAmount()).isEqualByComparingTo("116.00");
        assertThat(paymentRequestResponse.getAppliedExchangeRate()).isEqualByComparingTo(GBP_TO_EUR_RATE);
        assertThat(paymentRequestResponse.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(paymentRequestResponse.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(paymentRequestResponse.getCreatedAt()).isEqualTo(FETCHED_AT);
        assertThat(paymentRequestResponse.getMessage()).isEqualTo("submitted");
    }
}
