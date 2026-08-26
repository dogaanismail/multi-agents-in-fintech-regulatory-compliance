package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.enums.Currency;
import org.banksolution.integration.exchangerate.ExchangeRateApiClient;
import org.banksolution.integration.exchangerate.dto.ExchangeRateApiResponse;
import org.banksolution.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateSyncService {

    private static final String BASE_CURRENCY = "GBP";
    private static final int RATE_SCALE = 8;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateApiClient exchangeRateApiClient;
    private final Clock clock;

    @Transactional
    public void syncRates() {
        log.info("Syncing exchange rates from external provider using base currency: {}", BASE_CURRENCY);

        ExchangeRateApiResponse exchangeRateApiResponse = exchangeRateApiClient.fetchRates(BASE_CURRENCY);

        if (exchangeRateApiResponse == null || exchangeRateApiResponse.getRates() == null || exchangeRateApiResponse.getRates().isEmpty()) {
            log.warn("Exchange rate API returned empty or null response, skipping sync");
            return;
        }

        Map<String, BigDecimal> gbpRates = new HashMap<>(exchangeRateApiResponse.getRates());
        gbpRates.putIfAbsent(BASE_CURRENCY, BigDecimal.ONE);

        Instant fetchedAt = clock.instant();

        Map<String, ExchangeRateEntity> existingExchangeRateEntitiesByPair = exchangeRateRepository.findAll().stream()
                .collect(Collectors.toMap(ExchangeRateEntity::getCurrencyPair, exchangeRateEntity -> exchangeRateEntity));

        Currency[] currencies = Currency.values();
        List<ExchangeRateEntity> exchangeRateEntitiesToSave = new ArrayList<>();

        for (Currency fromCurrency : currencies) {
            for (Currency toCurrency : currencies) {
                if (fromCurrency == toCurrency) {
                    continue;
                }

                BigDecimal fromRate = gbpRates.get(fromCurrency.name());
                BigDecimal toRate = gbpRates.get(toCurrency.name());

                if (fromRate == null || toRate == null
                        || fromRate.compareTo(BigDecimal.ZERO) <= 0
                        || toRate.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Skipping pair {}{}: rate missing or invalid in API response", fromCurrency.name(), toCurrency.name());
                    continue;
                }

                BigDecimal rate = toRate.divide(fromRate, RATE_SCALE, RoundingMode.HALF_UP);
                String currencyPair = fromCurrency.name() + toCurrency.name();

                ExchangeRateEntity exchangeRateEntity = existingExchangeRateEntitiesByPair.containsKey(currencyPair)
                        ? existingExchangeRateEntitiesByPair.get(currencyPair)
                        : ExchangeRateEntity.builder().currencyPair(currencyPair).build();

                exchangeRateEntity.setRate(rate);
                exchangeRateEntity.setFetchedAt(fetchedAt);
                exchangeRateEntitiesToSave.add(exchangeRateEntity);
            }
        }

        exchangeRateRepository.saveAll(exchangeRateEntitiesToSave);
        log.info("Exchange rate sync completed: {} pairs saved", exchangeRateEntitiesToSave.size());
    }
}
