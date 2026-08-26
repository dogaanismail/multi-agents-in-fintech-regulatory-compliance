package org.banksolution.model;

import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CurrencyConversionTest {

    @Test
    void shouldScaleASameCurrencyAmountToTheCurrencyExponentWithoutARate() {
        CurrencyConversion currencyConversion = CurrencyConversion.sameCurrency(new BigDecimal("100.005"), Currency.GBP);

        assertThat(currencyConversion.sellAmount()).isEqualTo(new BigDecimal("100.01"));
        assertThat(currencyConversion.buyAmount()).isEqualTo(new BigDecimal("100.01"));
        assertThat(currencyConversion.sellCurrency()).isEqualTo(Currency.GBP);
        assertThat(currencyConversion.buyCurrency()).isEqualTo(Currency.GBP);
        assertThat(currencyConversion.exchangeRate()).isNull();
        assertThat(currencyConversion.fixedSide()).isEqualTo(FixedSide.SELL);
        assertThat(currencyConversion.requiresConversion()).isFalse();
    }

    @Test
    void shouldDropDecimalsForAZeroExponentCurrency() {
        assertThat(CurrencyConversion.sameCurrency(new BigDecimal("1000.4"), Currency.JPY).sellAmount())
                .isEqualTo(new BigDecimal("1000"));
    }

    @Test
    void shouldRequireConversionWhenTheCurrenciesDiffer() {
        CurrencyConversion currencyConversion = new CurrencyConversion(
                new BigDecimal("100.00"), Currency.GBP, new BigDecimal("116.00"), Currency.EUR, new BigDecimal("1.16"), FixedSide.SELL);

        assertThat(currencyConversion.requiresConversion()).isTrue();
    }
}
