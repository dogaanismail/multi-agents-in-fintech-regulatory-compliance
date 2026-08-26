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

import static org.banksolution.mapper.PaymentRequestMapper.toPaymentRequestEntity;
import static org.banksolution.mapper.PaymentRequestMapper.toPaymentRequestResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentCreatedEventProducer paymentCreatedEventProducer;
    private final AccountService accountService;
    private final CurrencyConversionService currencyConversionService;

    @Transactional
    public PaymentRequestResponse requestPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment request for customer: {}, type: {}, amount: {} {}",
                paymentRequest.getCustomerId(),
                paymentRequest.getPaymentType(),
                paymentRequest.getAmount(),
                paymentRequest.getFromCurrency());

        PaymentRequestUtil.validatePaymentRequest(paymentRequest);

        Optional<PaymentAccounts> resolvedPaymentAccounts = accountService.loadPaymentAccounts(
                paymentRequest.getSourceAccountId(),
                paymentRequest.getDestinationAccountId());

        CurrencyConversion currencyConversion = currencyConversionService.convert(
                paymentRequest.getAmount(),
                paymentRequest.getFromCurrency(),
                paymentRequest.getToCurrency(),
                paymentRequest.getFixedSide());

        PaymentRequestEntity paymentRequestEntity = toPaymentRequestEntity(paymentRequest);
        applyConversion(paymentRequestEntity, currencyConversion);
        paymentRequestEntity.setPaymentScheme(
                PaymentSchemeClassifier.classify(paymentRequest, resolvedPaymentAccounts.orElse(null)));

        PaymentRequestEntity savedPaymentRequestEntity = paymentRequestRepository
                .save(paymentRequestEntity);

        boolean isCrossBorderPayment = resolvedPaymentAccounts
                .map(accountService::isCrossBorderPayment)
                .orElse(false);

        //TODO: Investigate and implement outbox pattern
        paymentCreatedEventProducer.publishPaymentCreatedEvent(savedPaymentRequestEntity, isCrossBorderPayment);

        log.info("Payment request created: id:{}", savedPaymentRequestEntity.getId());
        return toPaymentRequestResponse(savedPaymentRequestEntity, "Payment request submitted successfully and is being processed");
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getPaymentsByCustomerId(UUID customerId) {
        log.info("Fetching payments for customer: {}", customerId);

        List<PaymentRequestEntity> paymentRequestEntities = paymentRequestRepository.findByCustomerId(customerId);

        return paymentRequestEntities.stream()
                .map(paymentRequestEntity -> toPaymentRequestResponse(paymentRequestEntity, null))
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
