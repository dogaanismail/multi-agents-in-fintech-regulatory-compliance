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

    @Transactional
    public void syncRates() {
        log.info("Syncing exchange rates from external provider using base currency: {}", BASE_CURRENCY);

        ExchangeRateApiResponse apiResponse = exchangeRateApiClient.fetchRates(BASE_CURRENCY);

        if (apiResponse == null || apiResponse.getRates() == null || apiResponse.getRates().isEmpty()) {
            log.warn("Exchange rate API returned empty or null response, skipping sync");
            return;
        }

        Map<String, BigDecimal> gbpRates = new HashMap<>(apiResponse.getRates());
        gbpRates.putIfAbsent(BASE_CURRENCY, BigDecimal.ONE);

        Instant fetchedAt = Instant.now();

        Map<String, ExchangeRateEntity> existing = exchangeRateRepository.findAll().stream()
                .collect(Collectors.toMap(ExchangeRateEntity::getCurrencyPair, e -> e));

        Currency[] currencies = Currency.values();
        List<ExchangeRateEntity> toSave = new ArrayList<>();

        for (Currency from : currencies) {
            for (Currency to : currencies) {
                if (from == to) {
                    continue;
                }

                BigDecimal fromRate = gbpRates.get(from.name());
                BigDecimal toRate = gbpRates.get(to.name());

                if (fromRate == null || toRate == null
                        || fromRate.compareTo(BigDecimal.ZERO) <= 0
                        || toRate.compareTo(BigDecimal.ZERO) <= 0) {
                    log.warn("Skipping pair {}{}: rate missing or invalid in API response", from.name(), to.name());
                    continue;
                }

                BigDecimal rate = toRate.divide(fromRate, RATE_SCALE, RoundingMode.HALF_UP);
                String currencyPair = from.name() + to.name();

                ExchangeRateEntity entity = existing.containsKey(currencyPair)
                        ? existing.get(currencyPair)
                        : ExchangeRateEntity.builder().currencyPair(currencyPair).build();

                entity.setRate(rate);
                entity.setFetchedAt(fetchedAt);
                toSave.add(entity);
            }
        }

        exchangeRateRepository.saveAll(toSave);
        log.info("Exchange rate sync completed: {} pairs saved", toSave.size());
    }
}
