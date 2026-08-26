package org.banksolution.mapper;

import org.banksolution.enums.Currency;
import org.banksolution.model.response.ExchangeRateResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.FETCHED_AT;
import static org.banksolution.fixtures.PaymentFixtures.createExchangeRateEntity;

class ExchangeRateMapperTest {

    @Test
    void shouldExposeThePairRateAndFetchTime() {
        ExchangeRateResponse exchangeRateResponse = ExchangeRateMapper.toExchangeRateResponse(
                createExchangeRateEntity(Currency.GBP, Currency.EUR, "1.16000000"));

        assertThat(exchangeRateResponse.getCurrencyPair()).isEqualTo("GBPEUR");
        assertThat(exchangeRateResponse.getRate()).isEqualByComparingTo("1.16");
        assertThat(exchangeRateResponse.getFetchedAt()).isEqualTo(FETCHED_AT);
    }
}
