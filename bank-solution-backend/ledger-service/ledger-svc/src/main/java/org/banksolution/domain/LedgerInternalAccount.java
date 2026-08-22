package org.banksolution.domain;

import lombok.Builder;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record LedgerInternalAccount(
        UUID id,
        LedgerAccountType accountType,
        Currency currency,
        BigDecimal creditsPosted,
        BigDecimal creditsPending,
        BigDecimal debitsPosted,
        BigDecimal debitsPending,
        Instant createdAt) {

    public BigDecimal netBalance() {
        return creditsPosted.subtract(debitsPosted);
    }

    public static LedgerInternalAccount newInternalAccount(
            LedgerAccountType accountType,
            Currency currency) {

        return LedgerInternalAccount.builder()
                .id(LedgerAccountIds.deriveInternalAccountId(accountType, currency))
                .accountType(accountType)
                .currency(currency)
                .build();
    }
}
