package org.banksolution.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Currency {

    AED(784, 2),
    ALL(8, 2),
    CHF(756, 2),
    EUR(978, 2),
    GBP(826, 2),
    INR(356, 2),
    JPY(392, 0),
    MAD(504, 2),
    MXN(484, 2),
    NGN(566, 2),
    PKR(586, 2),
    TRY(949, 2),
    USD(840, 2);

    private final int numericCode;
    private final int exponent;

    public static Currency fromNumericCode(int numericCode) {
        for (Currency currency : values()) {
            if (currency.numericCode == numericCode) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Unknown currency numeric code: " + numericCode);
    }
}
