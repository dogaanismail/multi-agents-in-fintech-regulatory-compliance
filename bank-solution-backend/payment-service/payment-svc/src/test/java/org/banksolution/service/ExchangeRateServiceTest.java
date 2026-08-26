package org.banksolution.service;

import org.banksolution.enums.Currency;
import org.banksolution.model.response.ExchangeRateResponse;
import org.banksolution.repository.ExchangeRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.createExchangeRateEntity;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    void shouldListEveryStoredRate() {
        when(exchangeRateRepository.findAll()).thenReturn(List.of(
                createExchangeRateEntity(Currency.GBP, Currency.EUR, "1.16"),
                createExchangeRateEntity(Currency.EUR, Currency.GBP, "0.86")));

        assertThat(exchangeRateService.getAllRates())
                .extracting(ExchangeRateResponse::getCurrencyPair)
                .containsExactly("GBPEUR", "EURGBP");
    }

    @Test
    void shouldLookUpAPairCaseInsensitively() {
        when(exchangeRateRepository.findByCurrencyPair("GBPEUR"))
                .thenReturn(Optional.of(createExchangeRateEntity(Currency.GBP, Currency.EUR, "1.16")));

        assertThat(exchangeRateService.getRate("gbp", "eur")).map(ExchangeRateResponse::getRate)
                .hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("1.16"));
        assertThat(exchangeRateService.getConversionRate("gbp", "eur"))
                .hasValueSatisfying(rate -> assertThat(rate).isEqualByComparingTo("1.16"));
    }

    @Test
    void shouldAnswerASameCurrencyLookupWithoutTouchingTheRepository() {
        assertThat(exchangeRateService.getRate("GBP", "gbp")).isEmpty();
        assertThat(exchangeRateService.getConversionRate("GBP", "gbp")).contains(BigDecimal.ONE);

        verifyNoInteractions(exchangeRateRepository);
    }

    @Test
    void shouldReturnEmptyForAnUnknownPair() {
        when(exchangeRateRepository.findByCurrencyPair("GBPJPY")).thenReturn(Optional.empty());

        assertThat(exchangeRateService.getRate("GBP", "JPY")).isEmpty();
        assertThat(exchangeRateService.getConversionRate("GBP", "JPY")).isEmpty();
    }
}
