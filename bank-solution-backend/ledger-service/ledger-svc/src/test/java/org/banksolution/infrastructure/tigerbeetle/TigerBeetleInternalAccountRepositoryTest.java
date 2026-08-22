package org.banksolution.infrastructure.tigerbeetle;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
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
                .persist(createInternalAccount(LedgerAccountType.SUSPENSE, Currency.GBP));

        assertThat(persisted.id())
                .isEqualTo(LedgerAccountIds.internal(LedgerAccountType.SUSPENSE, Currency.GBP));
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

        List<LedgerInternalAccount> first = tigerBeetleInternalAccountRepository.persistAll(chart);
        List<LedgerInternalAccount> second = tigerBeetleInternalAccountRepository.persistAll(chart);

        assertThat(first).hasSize(4);
        assertThat(second).extracting(LedgerInternalAccount::id)
                .containsExactlyInAnyOrderElementsOf(first.stream().map(LedgerInternalAccount::id).toList());
    }

    @Test
    void shouldLookUpInternalAccountsByDerivedIdWithoutARegistry() {
        List<UUID> derivedIds = List.of(
                LedgerAccountIds.internal(LedgerAccountType.INBOUND_CLEARING, Currency.EUR),
                LedgerAccountIds.internal(LedgerAccountType.OUTBOUND_CLEARING, Currency.EUR));

        assertThat(tigerBeetleInternalAccountRepository.findAll(derivedIds))
                .extracting(LedgerInternalAccount::accountType)
                .containsExactlyInAnyOrder(LedgerAccountType.INBOUND_CLEARING, LedgerAccountType.OUTBOUND_CLEARING);
    }
}
