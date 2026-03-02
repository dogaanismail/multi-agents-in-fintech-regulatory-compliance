package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentRequestEntity;
import org.banksolution.enums.Currency;
import org.banksolution.model.PaymentAccounts;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.producer.PaymentCreatedEventProducer;
import org.banksolution.repository.PaymentRequestRepository;
import org.banksolution.util.PaymentRequestUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ExchangeRateService exchangeRateService;

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

        PaymentRequestEntity entity = toEntity(request);
        applyConversionIfRequired(entity, request.getAmount());

        PaymentRequestEntity savedEntity = paymentRequestRepository.save(entity);

        boolean isCrossOrderPayment = paymentAccounts
                .map(accountService::isCrossOrderPayment)
                .orElse(false);

        //TODO: Investigate and implement outbox pattern
        paymentCreatedEventProducer.publishPaymentCreatedEvent(savedEntity, isCrossOrderPayment);

        log.info("Payment request created: id:{}", savedEntity.getId());
        return toResponse(savedEntity, "Payment request submitted successfully and is being processed");
    }

    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> getPaymentsByCustomerId(UUID customerId) {
        log.info("Fetching payments for customer: {}", customerId);

        List<PaymentRequestEntity> payments = paymentRequestRepository.findByCustomerId(customerId);

        return payments.stream()
                .map(entity -> toResponse(entity, null))
                .toList();
    }

    private void applyConversionIfRequired(PaymentRequestEntity entity, BigDecimal amount) {
        Currency fromCurrency = entity.getFromCurrency();
        Currency toCurrency = entity.getToCurrency();

        if (fromCurrency == toCurrency) {
            // Same-currency payment: no exchange needed, but convertedAmount must not be null
            entity.setConvertedAmount(amount);
            log.debug("Same-currency payment {}: convertedAmount set to amount {}", fromCurrency, amount);
            return;
        }

        try {
            String from = fromCurrency.name();
            String to = toCurrency.name();

            exchangeRateService.getConversionRate(from, to).ifPresentOrElse(
                    rate -> {
                        entity.setAppliedExchangeRate(rate);
                        entity.setConvertedAmount(amount.multiply(rate).setScale(4, RoundingMode.HALF_UP));
                        log.info("Currency conversion applied: {} {} -> {} (rate: {})",
                                amount, from, to, rate);
                    },
                    () -> {
                        // Rate unavailable — fall back to 1:1
                        entity.setConvertedAmount(amount);
                        log.warn("Exchange rate not available for {}->{}, using 1:1 fallback", from, to);
                    }
            );
        } catch (Exception e) {
            entity.setConvertedAmount(amount);
            log.warn("Failed to apply currency conversion, using 1:1 fallback: {}", e.getMessage(), e);
        }
    }
}
