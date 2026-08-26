package org.banksolution.mapper;

import com.aml.payment.FixedSide;
import com.aml.payment.PaymentCreatedEvent;
import com.aml.payment.PaymentScheme;
import com.aml.payment.PaymentType;
import org.banksolution.entity.PaymentRequestEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentCreatedEventMapperTest {

    @Test
    void shouldMapEveryFieldOfAnFxTransfer() {
        UUID paymentId = UUID.randomUUID();
        PaymentRequestEntity paymentRequestEntity = createPersistedPaymentRequestEntity(paymentId, CUSTOMER_ID);

        PaymentCreatedEvent paymentCreatedEvent = PaymentCreatedEventMapper.toPaymentCreatedEvent(paymentRequestEntity, true);

        assertThat(paymentCreatedEvent.getPaymentId()).isEqualTo(paymentId.toString());
        assertThat(paymentCreatedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(paymentCreatedEvent.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(paymentCreatedEvent.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
        assertThat(paymentCreatedEvent.getAmount()).isEqualTo("100.00");
        assertThat(paymentCreatedEvent.getFromCurrency()).isEqualTo("GBP");
        assertThat(paymentCreatedEvent.getToCurrency()).isEqualTo("EUR");
        assertThat(paymentCreatedEvent.getConvertedAmount()).isEqualTo("116.00");
        assertThat(paymentCreatedEvent.getAppliedExchangeRate()).isEqualTo("1.16000000");
        assertThat(paymentCreatedEvent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(paymentCreatedEvent.getPaymentScheme()).isEqualTo(PaymentScheme.INTERNAL_TRANSFER);
        assertThat(paymentCreatedEvent.getFixedSide()).isEqualTo(FixedSide.SELL);
        assertThat(paymentCreatedEvent.getIsCrossBorderPayment()).isTrue();
        assertThat(paymentCreatedEvent.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(paymentCreatedEvent.getEventId()).isNotBlank();
        assertThat(paymentCreatedEvent.getTimestamp()).isPositive();
    }

    @Test
    void shouldLeaveAbsentAccountsRateAndDescriptionAbsentForASameCurrencyDeposit() {
        PaymentRequestEntity depositEntity = createPersistedPaymentRequestEntity(UUID.randomUUID(), CUSTOMER_ID);
        depositEntity.setSourceAccountId(null);
        depositEntity.setDestinationAccountId(null);
        depositEntity.setAppliedExchangeRate(null);
        depositEntity.setDescription(null);
        depositEntity.setConvertedAmount(new BigDecimal("100.00"));

        PaymentCreatedEvent paymentCreatedEvent = PaymentCreatedEventMapper.toPaymentCreatedEvent(depositEntity, false);

        assertThat(paymentCreatedEvent.getSourceAccountId()).isNull();
        assertThat(paymentCreatedEvent.getDestinationAccountId()).isNull();
        assertThat(paymentCreatedEvent.getAppliedExchangeRate()).isNull();
        assertThat(paymentCreatedEvent.getDescription()).isNull();
        assertThat(paymentCreatedEvent.getIsCrossBorderPayment()).isFalse();
    }
}
