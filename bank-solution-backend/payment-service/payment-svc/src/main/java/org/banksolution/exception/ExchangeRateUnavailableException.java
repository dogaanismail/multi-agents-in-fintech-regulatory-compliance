package org.banksolution.exception;

import org.banksolution.enums.Currency;

public class ExchangeRateUnavailableException extends RuntimeException {
    public ExchangeRateUnavailableException(Currency sellCurrency, Currency buyCurrency) {
        super("No exchange rate available for " + sellCurrency + " to " + buyCurrency);
    }
}
