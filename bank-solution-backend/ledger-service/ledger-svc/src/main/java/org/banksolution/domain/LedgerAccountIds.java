package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LedgerAccountIds {

    private LedgerAccountIds() {
    }

    public static UUID deriveWalletAccountId(
            UUID customerAccountId,
            Currency currency) {

        return deriveFrom("wallet:" + customerAccountId + ":" + currency.name());
    }

    public static UUID deriveInternalAccountId(
            LedgerAccountType internalAccountType,
            Currency currency) {

        return deriveFrom("internal:" + internalAccountType.name() + ":" + currency.name());
    }

    private static UUID deriveFrom(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
