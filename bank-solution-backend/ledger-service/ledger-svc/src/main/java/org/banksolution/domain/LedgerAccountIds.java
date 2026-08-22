package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LedgerAccountIds {

    private LedgerAccountIds() {
    }

    public static UUID wallet(UUID accountId, Currency currency) {
        return derive("wallet:" + accountId + ":" + currency.name());
    }

    public static UUID internal(LedgerAccountType accountType, Currency currency) {
        return derive("internal:" + accountType.name() + ":" + currency.name());
    }

    private static UUID derive(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
