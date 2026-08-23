package org.banksolution.model;

import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CurrencyConversion(
        BigDecimal sellAmount,
        Currency sellCurrency,
        BigDecimal buyAmount,
        Currency buyCurrency,
        BigDecimal exchangeRate,
        FixedSide fixedSide) {

    public static CurrencyConversion sameCurrency(
            BigDecimal amount,
            Currency currency) {

        BigDecimal scaledAmount = amount.setScale(currency.getExponent(), RoundingMode.HALF_UP);

        return new CurrencyConversion(scaledAmount,
                currency,
                scaledAmount,
                currency,
                null,
                FixedSide.SELL);
    }

    public boolean requiresConversion() {
        return sellCurrency != buyCurrency;
    }
}
