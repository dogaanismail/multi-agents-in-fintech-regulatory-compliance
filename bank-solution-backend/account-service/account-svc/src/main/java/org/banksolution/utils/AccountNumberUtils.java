package org.banksolution.utils;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class AccountNumberUtils {

    public static String generateAccountNumber() {
        long accountNumber = ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
        return String.valueOf(accountNumber);
    }
}
