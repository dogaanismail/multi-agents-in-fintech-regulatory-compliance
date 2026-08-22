package org.banksolution.util;

import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyUtilsTest {

    @Test
    void shouldConvertTwoDecimalCurrencyToMinorUnits() {
        assertThat(MoneyUtils.toMinorUnits(new BigDecimal("1000.00"), Currency.GBP))
                .isEqualTo(BigInteger.valueOf(100_000));
    }

    @Test
    void shouldConvertZeroDecimalCurrencyToMinorUnits() {
        assertThat(MoneyUtils.toMinorUnits(new BigDecimal("1000"), Currency.JPY))
                .isEqualTo(BigInteger.valueOf(1_000));
    }

    @Test
    void shouldConvertMinorUnitsBackToAmount() {
        assertThat(MoneyUtils.toAmount(BigInteger.valueOf(100_000), Currency.GBP))
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void shouldRoundTripThroughMinorUnits() {
        BigDecimal amount = new BigDecimal("250.37");

        assertThat(MoneyUtils.toAmount(MoneyUtils.toMinorUnits(amount, Currency.GBP), Currency.GBP))
                .isEqualByComparingTo(amount);
    }

    @Test
    void shouldRejectAmountFinerThanTheCurrencyExponent() {
        BigDecimal amount = new BigDecimal("1.005");

        assertThatThrownBy(() -> MoneyUtils.toMinorUnits(amount, Currency.GBP))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void shouldRejectFractionalMinorUnitsForZeroDecimalCurrency() {
        BigDecimal amount = new BigDecimal("1.5");

        assertThatThrownBy(() -> MoneyUtils.toMinorUnits(amount, Currency.JPY))
                .isInstanceOf(ArithmeticException.class);
    }
}
