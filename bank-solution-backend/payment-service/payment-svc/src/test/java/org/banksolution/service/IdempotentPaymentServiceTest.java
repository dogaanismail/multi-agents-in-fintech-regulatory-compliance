package org.banksolution.service;

import org.banksolution.domain.PaymentIdempotency;
import org.banksolution.domain.PaymentRequestResult;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.enums.Currency;
import org.banksolution.exception.IdempotencyKeyReusedException;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.repository.PaymentRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.CUSTOMER_ID;
import static org.banksolution.fixtures.PaymentFixtures.PAYMENT_IDEMPOTENCY_KEY;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentIdempotency;
import static org.banksolution.fixtures.PaymentFixtures.createPersistedPaymentRequestEntity;
import static org.banksolution.fixtures.PaymentFixtures.createTransferOutRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentPaymentServiceTest {

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private IdempotentPaymentService idempotentPaymentService;

    @Test
    void shouldCreateThePaymentUnderItsDerivedIdTheFirstTimeAKeyIsSeen() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);
        PaymentRequestResponse createdPaymentRequestResponse = PaymentRequestResponse.builder().id(paymentIdempotency.paymentId()).build();
        when(paymentRequestRepository.findById(paymentIdempotency.paymentId())).thenReturn(Optional.empty());
        when(paymentService.createPayment(paymentIdempotency, paymentRequest)).thenReturn(createdPaymentRequestResponse);

        PaymentRequestResult paymentRequestResult = idempotentPaymentService.requestPayment(PAYMENT_IDEMPOTENCY_KEY, paymentRequest);

        assertThat(paymentRequestResult.replayed()).isFalse();
        assertThat(paymentRequestResult.paymentRequestResponse()).isSameAs(createdPaymentRequestResponse);
    }

    @Test
    void shouldReplayTheOriginalPaymentWithoutCreatingOrPublishingAgain() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);
        when(paymentRequestRepository.findById(paymentIdempotency.paymentId()))
                .thenReturn(Optional.of(createStoredPaymentRequestEntity(paymentIdempotency)));

        PaymentRequestResult paymentRequestResult = idempotentPaymentService.requestPayment(PAYMENT_IDEMPOTENCY_KEY, paymentRequest);

        assertThat(paymentRequestResult.replayed()).isTrue();
        assertThat(paymentRequestResult.paymentRequestResponse().getId()).isEqualTo(paymentIdempotency.paymentId());
        assertThat(paymentRequestResult.paymentRequestResponse().getMessage()).isEqualTo(PaymentService.PAYMENT_SUBMITTED_MESSAGE);
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldRejectAKeyReusedWithADifferentRequest() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);
        PaymentRequestEntity storedPaymentRequestEntity = createStoredPaymentRequestEntity(paymentIdempotency);
        storedPaymentRequestEntity.setRequestFingerprint("fingerprint-of-another-request");
        when(paymentRequestRepository.findById(paymentIdempotency.paymentId())).thenReturn(Optional.of(storedPaymentRequestEntity));

        assertThatThrownBy(() -> idempotentPaymentService.requestPayment(PAYMENT_IDEMPOTENCY_KEY, paymentRequest))
                .isInstanceOf(IdempotencyKeyReusedException.class)
                .hasMessageContaining(paymentIdempotency.paymentId().toString());
        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReplayTheWinnerWhenAConcurrentRetryCreatedThePaymentFirst() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);
        when(paymentRequestRepository.findById(paymentIdempotency.paymentId()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createStoredPaymentRequestEntity(paymentIdempotency)));
        when(paymentService.createPayment(paymentIdempotency, paymentRequest))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        PaymentRequestResult paymentRequestResult = idempotentPaymentService.requestPayment(PAYMENT_IDEMPOTENCY_KEY, paymentRequest);

        assertThat(paymentRequestResult.replayed()).isTrue();
        assertThat(paymentRequestResult.paymentRequestResponse().getId()).isEqualTo(paymentIdempotency.paymentId());
    }

    @Test
    void shouldRethrowAnIntegrityViolationThatIsNotADuplicateOfThisKey() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);
        DataIntegrityViolationException unrelatedIntegrityViolation = new DataIntegrityViolationException("check constraint violated");
        when(paymentRequestRepository.findById(paymentIdempotency.paymentId())).thenReturn(Optional.empty());
        when(paymentService.createPayment(any(PaymentIdempotency.class), any(PaymentRequest.class))).thenThrow(unrelatedIntegrityViolation);

        assertThatThrownBy(() -> idempotentPaymentService.requestPayment(PAYMENT_IDEMPOTENCY_KEY, paymentRequest))
                .isSameAs(unrelatedIntegrityViolation);
        verify(paymentService).createPayment(paymentIdempotency, paymentRequest);
    }

    private static PaymentRequestEntity createStoredPaymentRequestEntity(PaymentIdempotency paymentIdempotency) {
        PaymentRequestEntity paymentRequestEntity = createPersistedPaymentRequestEntity(paymentIdempotency.paymentId(), CUSTOMER_ID);
        paymentRequestEntity.setIdempotencyKey(paymentIdempotency.idempotencyKey());
        paymentRequestEntity.setRequestFingerprint(paymentIdempotency.requestFingerprint());
        return paymentRequestEntity;
    }
}
