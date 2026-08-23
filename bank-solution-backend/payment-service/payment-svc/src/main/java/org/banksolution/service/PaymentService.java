package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.model.CurrencyConversion;
import org.banksolution.model.PaymentAccounts;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.producer.PaymentCreatedEventProducer;
import org.banksolution.repository.PaymentRequestRepository;
import org.banksolution.util.PaymentRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.banksolution.mapper.PaymentRequestMapper.toEntity;
import static org.banksolution.mapper.PaymentRequestMapper.toResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentCreatedEventProducer paymentCreatedEventProducer;
    private final AccountService accountService;
    private final CurrencyConversionService currencyConversionService;

    @Transactional
    public PaymentRequestResponse requestPayment(PaymentRequest request) {
        log.info("Processing payment request for customer: {}, type: {}, amount: {} {}",
                request.getCustomerId(),
                request.getPaymentType(),
                request.getAmount(),
                request.getFromCurrency());

        PaymentRequestUtil.validatePaymentRequest(request);

        Optional<PaymentAccounts> paymentAccounts = accountService.loadPaymentAccounts(
                request.getSourceAccountId(),
                request.getDestinationAccountId());

        CurrencyConversion conversion = currencyConversionService.convert(
                request.getAmount(),
                request.getFromCurrency(),
                request.getToCurrency(),
                request.getFixedSide());

        PaymentRequestEntity paymentRequestEntity = toEntity(request);
        applyConversion(paymentRequestEntity, conversion);
        paymentRequestEntity.setPaymentScheme(
                PaymentSchemeClassifier.classify(request, paymentAccounts.orElse(null)));

        PaymentRequestEntity savedPaymentRequestEntity = paymentRequestRepository
                .save(paymentRequestEntity);

        boolean isCrossOrderPayment = paymentAccounts
                .map(accountService::isCrossOrderPayment)
                .orElse(false);

        //TODO: Investigate and implement outbox pattern
        paymentCreatedEventProducer.publishPaymentCreatedEvent(savedPaymentRequestEntity, isCrossOrderPayment);

        log.info("Payment request created: id:{}", savedPaymentRequestEntity.getId());
        return toResponse(savedPaymentRequestEntity, "Payment request submitted successfully and is being processed");
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getPaymentsByCustomerId(UUID customerId) {
        log.info("Fetching payments for customer: {}", customerId);

        List<PaymentRequestEntity> payments = paymentRequestRepository.findByCustomerId(customerId);

        return payments.stream()
                .map(entity -> toResponse(entity, null))
                .toList();
    }

    private static void applyConversion(
            PaymentRequestEntity paymentRequestEntity,
            CurrencyConversion currencyConversion) {

        paymentRequestEntity.setAmount(currencyConversion.sellAmount());
        paymentRequestEntity.setConvertedAmount(currencyConversion.buyAmount());
        paymentRequestEntity.setAppliedExchangeRate(currencyConversion.exchangeRate());
        paymentRequestEntity.setFixedSide(currencyConversion.fixedSide());
    }
}
