package org.banksolution.service;

import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.enums.Currency;
import org.banksolution.integration.exchangerate.ExchangeRateApiClient;
import org.banksolution.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.FETCHED_AT;
import static org.banksolution.fixtures.PaymentFixtures.createExchangeRateApiResponse;
import static org.banksolution.fixtures.PaymentFixtures.createExchangeRateEntity;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyRateSyncServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    private CurrencyRateSyncService createCurrencyRateSyncService() {
        return new CurrencyRateSyncService(exchangeRateRepository, exchangeRateApiClient, Clock.fixed(FETCHED_AT, ZoneOffset.UTC));
    }

    @Test
    void shouldDeriveEveryOrderedPairFromTheGbpBasedRatesAndStampTheFetchTimeFromTheClock() {
        when(exchangeRateApiClient.fetchRates("GBP")).thenReturn(createExchangeRateApiResponse(
                Map.of("EUR", new BigDecimal("1.16"), "USD", new BigDecimal("1.25"))));
        when(exchangeRateRepository.findAll()).thenReturn(List.of());

        createCurrencyRateSyncService().syncRates();

        List<ExchangeRateEntity> savedExchangeRateEntities = captureSavedRates();
        assertThat(savedExchangeRateEntities).hasSize(3 * 2);
        assertThat(savedExchangeRateEntities)
                .extracting(ExchangeRateEntity::getCurrencyPair)
                .containsExactlyInAnyOrder("GBPEUR", "GBPUSD", "EURGBP", "EURUSD", "USDGBP", "USDEUR");
        assertThat(rateOf(savedExchangeRateEntities, "GBPEUR")).isEqualByComparingTo("1.16");
        assertThat(rateOf(savedExchangeRateEntities, "EURGBP")).isEqualByComparingTo("0.86206897");
        assertThat(rateOf(savedExchangeRateEntities, "EURUSD")).isEqualByComparingTo("1.07758621");
        assertThat(savedExchangeRateEntities).allSatisfy(exchangeRateEntity ->
                assertThat(exchangeRateEntity.getFetchedAt()).isEqualTo(FETCHED_AT));
    }

    @Test
    void shouldUpdateExistingPairsInPlaceInsteadOfInsertingDuplicates() {
        ExchangeRateEntity existingGbpEurRate = createExchangeRateEntity(Currency.GBP, Currency.EUR, "1.10000000");
        when(exchangeRateApiClient.fetchRates("GBP")).thenReturn(createExchangeRateApiResponse(Map.of("EUR", new BigDecimal("1.16"))));
        when(exchangeRateRepository.findAll()).thenReturn(List.of(existingGbpEurRate));

        createCurrencyRateSyncService().syncRates();

        List<ExchangeRateEntity> savedExchangeRateEntities = captureSavedRates();
        assertThat(savedExchangeRateEntities).hasSize(2);
        assertThat(savedExchangeRateEntities).contains(existingGbpEurRate);
        assertThat(existingGbpEurRate.getRate()).isEqualByComparingTo("1.16");
    }

    @Test
    void shouldSkipPairsWhoseRateIsMissingZeroOrNegative() {
        Map<String, BigDecimal> gbpRates = new HashMap<>();
        gbpRates.put("EUR", new BigDecimal("1.16"));
        gbpRates.put("USD", BigDecimal.ZERO);
        gbpRates.put("JPY", new BigDecimal("-1"));
        when(exchangeRateApiClient.fetchRates("GBP")).thenReturn(createExchangeRateApiResponse(gbpRates));
        when(exchangeRateRepository.findAll()).thenReturn(List.of());

        createCurrencyRateSyncService().syncRates();

        assertThat(captureSavedRates())
                .extracting(ExchangeRateEntity::getCurrencyPair)
                .containsExactlyInAnyOrder("GBPEUR", "EURGBP");
    }

    @Test
    void shouldSkipTheSyncWhenTheProviderAnswersWithNothingUsable() {
        when(exchangeRateApiClient.fetchRates("GBP"))
                .thenReturn(null)
                .thenReturn(createExchangeRateApiResponse(null))
                .thenReturn(createExchangeRateApiResponse(Map.of()));
        CurrencyRateSyncService currencyRateSyncService = createCurrencyRateSyncService();

        currencyRateSyncService.syncRates();
        currencyRateSyncService.syncRates();
        currencyRateSyncService.syncRates();

        verify(exchangeRateRepository, never()).saveAll(anyList());
    }

    private List<ExchangeRateEntity> captureSavedRates() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExchangeRateEntity>> exchangeRateEntitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(exchangeRateEntitiesCaptor.capture());
        return exchangeRateEntitiesCaptor.getValue();
    }

    private static BigDecimal rateOf(List<ExchangeRateEntity> exchangeRateEntities, String currencyPair) {
        return exchangeRateEntities.stream()
                .filter(exchangeRateEntity -> exchangeRateEntity.getCurrencyPair().equals(currencyPair))
                .findFirst()
                .orElseThrow()
                .getRate();
    }
}
