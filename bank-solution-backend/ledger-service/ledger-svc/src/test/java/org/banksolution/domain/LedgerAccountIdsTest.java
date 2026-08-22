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
        assertThat(LedgerAccountIds.wallet(ACCOUNT_ID, Currency.GBP))
                .isEqualTo(LedgerAccountIds.wallet(ACCOUNT_ID, Currency.GBP));
    }

    @Test
    void shouldDeriveDistinctWalletIdsPerCurrency() {
        assertThat(LedgerAccountIds.wallet(ACCOUNT_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.wallet(ACCOUNT_ID, Currency.EUR));
    }

    @Test
    void shouldDeriveDistinctWalletIdsPerAccount() {
        assertThat(LedgerAccountIds.wallet(ACCOUNT_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.wallet(UUID.randomUUID(), Currency.GBP));
    }

    @Test
    void shouldDeriveTheSameInternalIdForTheSameInputs() {
        assertThat(LedgerAccountIds.internal(LedgerAccountType.INBOUND_CLEARING, Currency.GBP))
                .isEqualTo(LedgerAccountIds.internal(LedgerAccountType.INBOUND_CLEARING, Currency.GBP));
    }

    @Test
    void shouldDeriveDistinctInternalIdsPerType() {
        assertThat(LedgerAccountIds.internal(LedgerAccountType.INBOUND_CLEARING, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.internal(LedgerAccountType.OUTBOUND_CLEARING, Currency.GBP));
    }

    @Test
    void shouldNotCollideBetweenWalletAndInternalNamespaces() {
        assertThat(LedgerAccountIds.wallet(ACCOUNT_ID, Currency.GBP))
                .isNotEqualTo(LedgerAccountIds.internal(LedgerAccountType.SUSPENSE, Currency.GBP));
    }
}
