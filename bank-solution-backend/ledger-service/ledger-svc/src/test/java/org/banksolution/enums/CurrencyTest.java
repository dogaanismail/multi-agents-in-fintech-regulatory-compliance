package org.banksolution.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyTest {

    private static final int UNKNOWN_NUMERIC_CODE = 1;

    @Test
    void shouldExposeIso4217NumericCodes() {
        assertThat(Currency.GBP.getNumericCode()).isEqualTo(826);
        assertThat(Currency.EUR.getNumericCode()).isEqualTo(978);
        assertThat(Currency.USD.getNumericCode()).isEqualTo(840);
        assertThat(Currency.JPY.getNumericCode()).isEqualTo(392);
    }

    @Test
    void shouldExposeMinorUnitExponents() {
        assertThat(Currency.GBP.getExponent()).isEqualTo(2);
        assertThat(Currency.JPY.getExponent()).isZero();
    }

    @Test
    void shouldHaveUniqueNumericCodes() {
        assertThat(Currency.values())
                .extracting(Currency::getNumericCode)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldNeverUseZeroAsNumericCode() {
        assertThat(Currency.values()).allMatch(currency -> currency.getNumericCode() > 0);
    }

    @Test
    void shouldResolveEveryCurrencyFromItsNumericCode() {
        assertThat(Arrays.stream(Currency.values()))
                .allSatisfy(currency ->
                        assertThat(Currency.fromNumericCode(currency.getNumericCode())).isEqualTo(currency));
    }

    @Test
    void shouldRejectUnknownNumericCode() {
        assertThatThrownBy(() -> Currency.fromNumericCode(UNKNOWN_NUMERIC_CODE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown currency numeric code");
    }
}
