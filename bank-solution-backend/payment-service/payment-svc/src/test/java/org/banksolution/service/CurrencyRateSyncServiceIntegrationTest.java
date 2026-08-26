package org.banksolution.service;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.enums.Currency;
import org.banksolution.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.common.initializers.WireMockInitializer.EXCHANGE_RATE_API_BASE_PATH;

class CurrencyRateSyncServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CurrencyRateSyncService currencyRateSyncService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Test
    void shouldPullGbpBasedRatesFromTheProviderAndStoreEveryOrderedPair() {
        stubFor(get(urlEqualTo(EXCHANGE_RATE_API_BASE_PATH + "/GBP"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"base":"GBP","date":"2026-08-27","time_last_updated":1787000000,
                                 "rates":{"GBP":1,"EUR":1.16,"USD":1.25,"JPY":190.5}}
                                """)));

        currencyRateSyncService.syncRates();

        assertThat(exchangeRateRepository.findByCurrencyPair("GBPJPY")).map(ExchangeRateEntity::getRate)
                .hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("190.5"));
        assertThat(exchangeRateRepository.findByCurrencyPair("JPYGBP")).map(ExchangeRateEntity::getRate)
                .hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo(BigDecimal.ONE.divide(new BigDecimal("190.5"), 8, java.math.RoundingMode.HALF_UP)));
        assertThat(exchangeRateRepository.findByCurrencyPair("USDEUR")).isPresent();
        assertThat(exchangeRateRepository.findByCurrencyPair(Currency.GBP.name() + Currency.NGN.name())).isEmpty();
    }
}
