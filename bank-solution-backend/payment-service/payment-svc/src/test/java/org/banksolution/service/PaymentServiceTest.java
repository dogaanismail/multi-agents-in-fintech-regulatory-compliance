package org.banksolution.service;

import org.banksolution.domain.PaymentIdempotency;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.model.CurrencyConversion;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.producer.PaymentCreatedEventProducer;
import org.banksolution.repository.PaymentRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Mock
    private PaymentCreatedEventProducer paymentCreatedEventProducer;

    @Mock
    private AccountService accountService;

    @Mock
    private CurrencyConversionService currencyConversionService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldConvertClassifyPersistAndPublishACrossBorderInternalTransfer() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.EUR);
        when(accountService.loadPaymentAccounts(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID))
                .thenReturn(Optional.of(createPaymentAccounts("GB", "DE")));
        when(accountService.isCrossBorderPayment(any())).thenReturn(true);
        when(currencyConversionService.convert(AMOUNT, Currency.GBP, Currency.EUR, FixedSide.SELL)).thenReturn(
                new CurrencyConversion(AMOUNT, Currency.GBP, new BigDecimal("116.00"), Currency.EUR, GBP_TO_EUR_RATE, FixedSide.SELL));
        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);
        when(paymentRequestRepository.saveAndFlush(any(PaymentRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequestResponse paymentRequestResponse = paymentService.createPayment(paymentIdempotency, paymentRequest);

        ArgumentCaptor<PaymentRequestEntity> paymentRequestEntityCaptor = ArgumentCaptor.forClass(PaymentRequestEntity.class);
        verify(paymentRequestRepository).saveAndFlush(paymentRequestEntityCaptor.capture());

        PaymentRequestEntity savedPaymentRequestEntity = paymentRequestEntityCaptor.getValue();
        assertThat(savedPaymentRequestEntity.getPaymentScheme()).isEqualTo(PaymentScheme.INTERNAL_TRANSFER);
        assertThat(savedPaymentRequestEntity.getConvertedAmount()).isEqualByComparingTo("116.00");
        assertThat(savedPaymentRequestEntity.getAppliedExchangeRate()).isEqualByComparingTo(GBP_TO_EUR_RATE);
        assertThat(savedPaymentRequestEntity.getFixedSide()).isEqualTo(FixedSide.SELL);

        verify(paymentCreatedEventProducer).publishPaymentCreatedEvent(savedPaymentRequestEntity, true);
        assertThat(paymentRequestResponse.getId()).isEqualTo(paymentIdempotency.paymentId());
        assertThat(savedPaymentRequestEntity.getIdempotencyKey()).isEqualTo(paymentIdempotency.idempotencyKey());
        assertThat(savedPaymentRequestEntity.getRequestFingerprint()).isEqualTo(paymentIdempotency.requestFingerprint());
        assertThat(paymentRequestResponse.getMessage()).isEqualTo(PaymentService.PAYMENT_SUBMITTED_MESSAGE);
    }

    @Test
    void shouldBookAnExternalInboundDepositWithoutConsultingTheAccountService() {
        PaymentRequest depositRequest = createDepositRequest(CUSTOMER_ID);
        when(accountService.loadPaymentAccounts(null, DESTINATION_ACCOUNT_ID)).thenReturn(Optional.empty());
        when(currencyConversionService.convert(AMOUNT, Currency.GBP, Currency.GBP, FixedSide.SELL))
                .thenReturn(CurrencyConversion.sameCurrency(AMOUNT, Currency.GBP));
        when(paymentRequestRepository.saveAndFlush(any(PaymentRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.createPayment(createPaymentIdempotency(depositRequest), depositRequest);

        ArgumentCaptor<PaymentRequestEntity> paymentRequestEntityCaptor = ArgumentCaptor.forClass(PaymentRequestEntity.class);
        verify(paymentRequestRepository).saveAndFlush(paymentRequestEntityCaptor.capture());
        assertThat(paymentRequestEntityCaptor.getValue().getPaymentScheme()).isEqualTo(PaymentScheme.EXTERNAL_INBOUND);
        assertThat(paymentRequestEntityCaptor.getValue().getAppliedExchangeRate()).isNull();
        verify(paymentCreatedEventProducer).publishPaymentCreatedEvent(paymentRequestEntityCaptor.getValue(), false);
    }

    @Test
    void shouldRejectADebitWithoutASourceAccountBeforeTouchingAnyCollaborator() {
        PaymentRequest paymentRequest = createTransferOutRequest(CUSTOMER_ID, Currency.GBP, Currency.GBP);
        paymentRequest.setSourceAccountId(null);

        PaymentIdempotency paymentIdempotency = createPaymentIdempotency(paymentRequest);

        assertThatThrownBy(() -> paymentService.createPayment(paymentIdempotency, paymentRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Source account is required for TRANSFER_OUT");

        verifyNoInteractions(accountService, currencyConversionService, paymentRequestRepository, paymentCreatedEventProducer);
    }

    @Test
    void shouldListACustomersPaymentsWithoutAMessage() {
        when(paymentRequestRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of(
                createPersistedPaymentRequestEntity(UUID.randomUUID(), CUSTOMER_ID),
                createPersistedPaymentRequestEntity(UUID.randomUUID(), CUSTOMER_ID)));

        List<PaymentRequestResponse> paymentRequestResponses = paymentService.getPaymentsByCustomerId(CUSTOMER_ID);

        assertThat(paymentRequestResponses).hasSize(2)
                .allSatisfy(paymentRequestResponse -> {
                    assertThat(paymentRequestResponse.getCustomerId()).isEqualTo(CUSTOMER_ID);
                    assertThat(paymentRequestResponse.getMessage()).isNull();
                });
    }
}
