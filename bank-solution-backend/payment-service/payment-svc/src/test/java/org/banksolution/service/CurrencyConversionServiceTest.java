package org.banksolution.service;

import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.banksolution.exception.ExchangeRateUnavailableException;
import org.banksolution.model.CurrencyConversion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    private static final BigDecimal GBP_TO_EUR = new BigDecimal("1.16000000");
    private static final BigDecimal GBP_TO_JPY = new BigDecimal("190.12345678");

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private CurrencyConversionService currencyConversionService;

    @Test
    void shouldNotLookUpARateForASameCurrencyPayment() {
        CurrencyConversion currencyConversion =
                currencyConversionService.convert(new BigDecimal("100"), Currency.GBP, Currency.GBP, FixedSide.BUY);

        assertThat(currencyConversion.sellAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(currencyConversion.exchangeRate()).isNull();
        assertThat(currencyConversion.fixedSide()).isEqualTo(FixedSide.SELL);
        verifyNoInteractions(exchangeRateService);
    }

    @Test
    void shouldFixTheSellSideAndDeriveTheBuyAmountInTheTargetCurrencyScale() {
        when(exchangeRateService.getConversionRate("GBP", "JPY")).thenReturn(Optional.of(GBP_TO_JPY));

        CurrencyConversion currencyConversion =
                currencyConversionService.convert(new BigDecimal("100.00"), Currency.GBP, Currency.JPY, FixedSide.SELL);

        assertThat(currencyConversion.sellAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(currencyConversion.buyAmount()).isEqualTo(new BigDecimal("19012"));
        assertThat(currencyConversion.buyCurrency()).isEqualTo(Currency.JPY);
        assertThat(currencyConversion.exchangeRate()).isEqualByComparingTo(GBP_TO_JPY);
        assertThat(currencyConversion.fixedSide()).isEqualTo(FixedSide.SELL);
    }

    @Test
    void shouldFixTheBuySideAndDeriveTheSellAmountByDividingThroughTheRate() {
        when(exchangeRateService.getConversionRate("GBP", "EUR")).thenReturn(Optional.of(GBP_TO_EUR));

        CurrencyConversion currencyConversion =
                currencyConversionService.convert(new BigDecimal("116.00"), Currency.GBP, Currency.EUR, FixedSide.BUY);

        assertThat(currencyConversion.buyAmount()).isEqualTo(new BigDecimal("116.00"));
        assertThat(currencyConversion.sellAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(currencyConversion.fixedSide()).isEqualTo(FixedSide.BUY);
    }

    @Test
    void shouldTreatAMissingFixedSideAsSellFixed() {
        when(exchangeRateService.getConversionRate("GBP", "EUR")).thenReturn(Optional.of(GBP_TO_EUR));

        CurrencyConversion currencyConversion =
                currencyConversionService.convert(new BigDecimal("100.00"), Currency.GBP, Currency.EUR, null);

        assertThat(currencyConversion.sellAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(currencyConversion.buyAmount()).isEqualTo(new BigDecimal("116.00"));
        assertThat(currencyConversion.fixedSide()).isEqualTo(FixedSide.SELL);
    }

    @Test
    void shouldRefuseToConvertWithoutARate() {
        when(exchangeRateService.getConversionRate("GBP", "EUR")).thenReturn(Optional.empty());
        BigDecimal amount = new BigDecimal("100.00");

        assertThatThrownBy(() -> currencyConversionService.convert(amount, Currency.GBP, Currency.EUR, FixedSide.SELL))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessage("No exchange rate available for GBP to EUR");
    }
}
