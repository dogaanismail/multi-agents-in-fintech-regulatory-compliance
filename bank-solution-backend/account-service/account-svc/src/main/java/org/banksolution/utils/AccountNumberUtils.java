package org.banksolution.utils;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

@UtilityClass
public class AccountNumberUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long MIN_ACCOUNT_NUMBER = 1_000_000_000L;
    private static final long MAX_ACCOUNT_NUMBER = 10_000_000_000L;

    public static String generateAccountNumber() {
        return String.valueOf(SECURE_RANDOM.nextLong(MIN_ACCOUNT_NUMBER, MAX_ACCOUNT_NUMBER));
    }
}
