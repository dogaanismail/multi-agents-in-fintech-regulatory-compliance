package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.enums.Currency;
import org.banksolution.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.banksolution.fixtures.PaymentFixtures.createExchangeRateEntity;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExchangeRateControllerTest extends BaseIntegrationTest {

    private static final String EXCHANGE_RATES_URL = "/api/v1/exchange-rates";

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @BeforeEach
    void givenGbpToUsdRate() {
        if (exchangeRateRepository.findByCurrencyPair("GBPUSD").isEmpty()) {
            exchangeRateRepository.saveAndFlush(createExchangeRateEntity(Currency.GBP, Currency.USD, "1.25000000"));
        }
    }

    @Test
    void shouldListStoredRates() throws Exception {
        mockMvc.perform(get(EXCHANGE_RATES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldReturnAPairRegardlessOfCase() throws Exception {
        mockMvc.perform(get(EXCHANGE_RATES_URL + "/gbp/usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyPair").value("GBPUSD"))
                .andExpect(jsonPath("$.rate").value(1.25));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownOrSameCurrencyPair() throws Exception {
        mockMvc.perform(get(EXCHANGE_RATES_URL + "/GBP/ALL")).andExpect(status().isNotFound());
        mockMvc.perform(get(EXCHANGE_RATES_URL + "/GBP/GBP")).andExpect(status().isNotFound());
    }
}
