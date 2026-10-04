package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.model.request.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.CUSTOMER_ID;
import static org.banksolution.fixtures.PaymentFixtures.PAYMENT_IDEMPOTENCY_KEY;
import static org.banksolution.fixtures.PaymentFixtures.createTransferOutRequest;

class PaymentIdempotencyTest {

    @Test
    void shouldDeriveTheSamePaymentIdForEveryRetryOfOneKey() {
        assertThat(PaymentIdempotency.derivePaymentId(CUSTOMER_ID, PAYMENT_IDEMPOTENCY_KEY))
                .isEqualTo(PaymentIdempotency.derivePaymentId(CUSTOMER_ID, PAYMENT_IDEMPOTENCY_KEY));
    }

    @Test
    void shouldScopeTheKeyToTheCustomerSoTwoCustomersNeverCollide() {
        assertThat(PaymentIdempotency.derivePaymentId(CUSTOMER_ID, PAYMENT_IDEMPOTENCY_KEY))
                .isNotEqualTo(PaymentIdempotency.derivePaymentId(UUID.randomUUID(), PAYMENT_IDEMPOTENCY_KEY))
                .isNotEqualTo(PaymentIdempotency.derivePaymentId(CUSTOMER_ID, PAYMENT_IDEMPOTENCY_KEY + "-2"));
    }

    @Test
    void shouldFingerprintEqualRequestsIdenticallyRegardlessOfAmountScale() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentRequest rescaledPaymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        rescaledPaymentRequest.setAmount(new BigDecimal("100.0000"));

        assertThat(PaymentIdempotency.derivePaymentRequestFingerprint(rescaledPaymentRequest))
                .isEqualTo(PaymentIdempotency.derivePaymentRequestFingerprint(paymentRequest))
                .hasSize(64);
    }

    @Test
    void shouldFingerprintADifferentAmountDifferently() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentRequest changedPaymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        changedPaymentRequest.setAmount(new BigDecimal("100.01"));

        assertThat(PaymentIdempotency.derivePaymentRequestFingerprint(changedPaymentRequest))
                .isNotEqualTo(PaymentIdempotency.derivePaymentRequestFingerprint(paymentRequest));
    }

    @Test
    void shouldCarryTheKeyTheIdAndTheFingerprintTogether() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);

        PaymentIdempotency paymentIdempotency = PaymentIdempotency.derivePaymentIdempotency(PAYMENT_IDEMPOTENCY_KEY, paymentRequest);

        assertThat(paymentIdempotency.paymentId()).isEqualTo(PaymentIdempotency.derivePaymentId(CUSTOMER_ID, PAYMENT_IDEMPOTENCY_KEY));
        assertThat(paymentIdempotency.idempotencyKey()).isEqualTo(PAYMENT_IDEMPOTENCY_KEY);
        assertThat(paymentIdempotency.requestFingerprint()).isEqualTo(PaymentIdempotency.derivePaymentRequestFingerprint(paymentRequest));
    }

    @Test
    void shouldRejectABlankOrOverlongKey() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        String overlongIdempotencyKey = "k".repeat(PaymentIdempotency.MAX_IDEMPOTENCY_KEY_LENGTH + 1);

        assertThatThrownBy(() -> PaymentIdempotency.derivePaymentIdempotency(" ", paymentRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency-Key must be between 1 and 255 characters");
        assertThatThrownBy(() -> PaymentIdempotency.derivePaymentIdempotency(overlongIdempotencyKey, paymentRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
