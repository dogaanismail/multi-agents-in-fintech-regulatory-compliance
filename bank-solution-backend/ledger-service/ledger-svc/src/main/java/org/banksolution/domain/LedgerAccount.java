package org.banksolution.domain;

import lombok.Builder;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record LedgerAccount(
        UUID id,
        UUID accountId,
        LedgerAccountType accountType,
        Currency currency,
        BigDecimal creditsPosted,
        BigDecimal creditsPending,
        BigDecimal debitsPosted,
        BigDecimal debitsPending,
        Instant createdAt) {

    public BigDecimal availableBalance() {
        return creditsPosted.subtract(debitsPosted).subtract(debitsPending);
    }

    public BigDecimal netBalance() {
        return creditsPosted.subtract(debitsPosted);
    }

    public static LedgerAccount newWallet(
            UUID accountId,
            Currency currency) {

        return LedgerAccount.builder()
                .id(LedgerAccountIds.deriveWalletAccountId(accountId, currency))
                .accountId(accountId)
                .accountType(LedgerAccountType.WALLET)
                .currency(currency)
                .build();
    }
}
