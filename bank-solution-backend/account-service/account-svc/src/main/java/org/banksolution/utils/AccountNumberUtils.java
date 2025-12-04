package org.banksolution.utils;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class AccountNumberUtils {

    public static String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
