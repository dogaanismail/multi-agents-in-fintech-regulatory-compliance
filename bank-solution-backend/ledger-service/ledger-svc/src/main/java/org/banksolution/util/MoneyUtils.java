package org.banksolution.util;

import org.banksolution.enums.Currency;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static BigInteger toMinorUnits(BigDecimal amount, Currency currency) {
        return amount.movePointRight(currency.getExponent()).toBigIntegerExact();
    }

    public static BigDecimal toAmount(BigInteger minorUnits, Currency currency) {
        return new BigDecimal(minorUnits, currency.getExponent());
    }
}
