package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.banksolution.exception.ExchangeRateUnavailableException;
import org.banksolution.model.CurrencyConversion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionService {

    private final ExchangeRateService exchangeRateService;

    public CurrencyConversion convert(
            BigDecimal amount,
            Currency sellCurrency,
            Currency buyCurrency,
            FixedSide fixedSide) {

        if (sellCurrency == buyCurrency) {
            return CurrencyConversion.sameCurrency(amount, sellCurrency);
        }

        BigDecimal rate = exchangeRateService.getConversionRate(sellCurrency.name(), buyCurrency.name())
                .orElseThrow(() -> new ExchangeRateUnavailableException(sellCurrency, buyCurrency));

        return fixedSide == FixedSide.SELL
                ? sellSideFixed(amount, sellCurrency, buyCurrency, rate)
                : buySideFixed(amount, sellCurrency, buyCurrency, rate);
    }

    private static CurrencyConversion sellSideFixed(
            BigDecimal sellAmount,
            Currency sellCurrency,
            Currency buyCurrency,
            BigDecimal rate) {

        BigDecimal buyAmount = scaleToCurrency(sellAmount.multiply(rate), buyCurrency);

        return new CurrencyConversion(
                scaleToCurrency(sellAmount, sellCurrency),
                sellCurrency,
                buyAmount,
                buyCurrency,
                rate,
                FixedSide.SELL);
    }

    private static CurrencyConversion buySideFixed(
            BigDecimal buyAmount,
            Currency sellCurrency,
            Currency buyCurrency,
            BigDecimal rate) {

        BigDecimal sellAmount = scaleToCurrency(
                buyAmount.divide(rate, buyCurrency.getExponent() + 6, RoundingMode.HALF_UP), sellCurrency);

        return new CurrencyConversion(
                sellAmount,
                sellCurrency,
                scaleToCurrency(buyAmount, buyCurrency),
                buyCurrency,
                rate,
                FixedSide.BUY);
    }

    private static BigDecimal scaleToCurrency(
            BigDecimal amount,
            Currency currency) {

        return amount.setScale(currency.getExponent(), RoundingMode.HALF_UP);
    }
}
