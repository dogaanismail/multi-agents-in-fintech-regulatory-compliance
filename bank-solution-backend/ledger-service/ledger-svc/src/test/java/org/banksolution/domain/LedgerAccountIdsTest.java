package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerAccountIdsTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Test
    void shouldDeriveTheSameWalletIdForTheSameInputs() {
        assertThat(LedgerAccountIds.deriveWalletAccountId(ACCOUNT_ID, Currency.GBP))
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(ACCOUNT_ID, Currency.GBP));
    }

    @Test
    void shouldDeriveDistinctWalletIdsPerCurrency() {
        assertThat(LedgerAccountIds.deriveWalletAccountId(ACCOUNT_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.deriveWalletAccountId(ACCOUNT_ID, Currency.EUR));
    }

    @Test
    void shouldDeriveDistinctWalletIdsPerAccount() {
        assertThat(LedgerAccountIds.deriveWalletAccountId(ACCOUNT_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.deriveWalletAccountId(UUID.randomUUID(), Currency.GBP));
    }

    @Test
    void shouldDeriveTheSameInternalIdForTheSameInputs() {
        assertThat(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.INBOUND_CLEARING, Currency.GBP))
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.INBOUND_CLEARING, Currency.GBP));
    }

    @Test
    void shouldDeriveDistinctInternalIdsPerType() {
        assertThat(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.INBOUND_CLEARING, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.OUTBOUND_CLEARING, Currency.GBP));
    }

    @Test
    void shouldNotCollideBetweenWalletAndInternalNamespaces() {
        assertThat(LedgerAccountIds.deriveWalletAccountId(ACCOUNT_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.SUSPENSE, Currency.GBP));
    }
}
