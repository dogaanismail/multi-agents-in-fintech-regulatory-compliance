package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.mapper.ExchangeRateMapper;
import org.banksolution.model.response.ExchangeRateResponse;
import org.banksolution.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getAllRates() {
        return exchangeRateRepository.findAll()
                .stream()
                .map(ExchangeRateMapper::toExchangeRateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ExchangeRateResponse> getRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Optional.empty();
        }
        String currencyPair = fromCurrency.toUpperCase() + toCurrency.toUpperCase();
        return exchangeRateRepository.findByCurrencyPair(currencyPair)
                .map(ExchangeRateMapper::toExchangeRateResponse);
    }

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getConversionRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Optional.of(BigDecimal.ONE);
        }

        String currencyPair = fromCurrency.toUpperCase() + toCurrency.toUpperCase();
        return exchangeRateRepository.findByCurrencyPair(currencyPair)
                .map(ExchangeRateEntity::getRate);
    }
}
