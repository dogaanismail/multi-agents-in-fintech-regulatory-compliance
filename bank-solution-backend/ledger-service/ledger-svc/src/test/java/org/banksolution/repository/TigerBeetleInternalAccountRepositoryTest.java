package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.banksolution.exception.LedgerAccountPersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.LedgerAccountFixtures.createInternalAccount;

class TigerBeetleInternalAccountRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TigerBeetleInternalAccountRepository tigerBeetleInternalAccountRepository;

    @Test
    void shouldPersistAndRetrieveInternalAccount() {
        LedgerInternalAccount persisted = tigerBeetleInternalAccountRepository
                .persistInternalAccount(createInternalAccount(LedgerAccountType.SUSPENSE, Currency.GBP));

        assertThat(persisted.id())
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.SUSPENSE, Currency.GBP));
        assertThat(persisted.accountType()).isEqualTo(LedgerAccountType.SUSPENSE);
        assertThat(persisted.currency()).isEqualTo(Currency.GBP);
        assertThat(persisted.netBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldSeedTheWholeChartOfAccountsIdempotently() {
        List<LedgerInternalAccount> chart = List.of(
                createInternalAccount(LedgerAccountType.INBOUND_CLEARING, Currency.USD),
                createInternalAccount(LedgerAccountType.OUTBOUND_CLEARING, Currency.USD),
                createInternalAccount(LedgerAccountType.FEES_INCOME, Currency.USD),
                createInternalAccount(LedgerAccountType.SUSPENSE, Currency.USD));

        List<LedgerInternalAccount> first = tigerBeetleInternalAccountRepository.persistInternalAccounts(chart);
        List<LedgerInternalAccount> second = tigerBeetleInternalAccountRepository.persistInternalAccounts(chart);

        assertThat(first).hasSize(4);
        assertThat(second).extracting(LedgerInternalAccount::id)
                .containsExactlyInAnyOrderElementsOf(first.stream().map(LedgerInternalAccount::id).toList());
    }

    @Test
    void shouldLookUpInternalAccountsByDerivedIdWithoutARegistry() {
        List<UUID> derivedIds = List.of(
                LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.INBOUND_CLEARING, Currency.EUR),
                LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.OUTBOUND_CLEARING, Currency.EUR));

        assertThat(tigerBeetleInternalAccountRepository.findInternalAccountsByIds(derivedIds))
                .extracting(LedgerInternalAccount::accountType)
                .containsExactlyInAnyOrder(LedgerAccountType.INBOUND_CLEARING, LedgerAccountType.OUTBOUND_CLEARING);
    }

    @Test
    void shouldRefuseToReuseAnInternalAccountIdOnADifferentLedger() {
        LedgerInternalAccount gbpSuspenseAccount = tigerBeetleInternalAccountRepository.persistInternalAccount(
                LedgerInternalAccount.newInternalAccount(LedgerAccountType.SUSPENSE, Currency.GBP));
        LedgerInternalAccount conflictingAccount = LedgerInternalAccount.builder()
                .id(gbpSuspenseAccount.id())
                .accountType(LedgerAccountType.SUSPENSE)
                .currency(Currency.EUR)
                .build();

        assertThatThrownBy(() -> tigerBeetleInternalAccountRepository.persistInternalAccount(conflictingAccount))
                .isInstanceOf(LedgerAccountPersistenceException.class)
                .hasMessageContaining("ExistsWithDifferentLedger");
    }

    @Test
    void shouldShortCircuitEmptyLookupsAndPersists() {
        assertThat(tigerBeetleInternalAccountRepository.findInternalAccountsByIds(List.of())).isEmpty();
        assertThat(tigerBeetleInternalAccountRepository.persistInternalAccounts(List.of())).isEmpty();
    }
}
