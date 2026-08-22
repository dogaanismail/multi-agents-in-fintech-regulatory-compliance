package org.banksolution.infrastructure.tigerbeetle;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.LedgerAccountFixtures.createWallet;

class TigerBeetleAccountRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TigerBeetleAccountRepository tigerBeetleAccountRepository;

    @Test
    void shouldPersistAndRetrieveWalletAccount() {
        UUID accountId = UUID.randomUUID();

        LedgerAccount persisted = tigerBeetleAccountRepository.persist(createWallet(accountId, Currency.GBP));

        assertThat(persisted.id()).isEqualTo(LedgerAccountIds.wallet(accountId, Currency.GBP));
        assertThat(persisted.accountId()).isEqualTo(accountId);
        assertThat(persisted.accountType()).isEqualTo(LedgerAccountType.WALLET);
        assertThat(persisted.currency()).isEqualTo(Currency.GBP);
        assertThat(persisted.availableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(persisted.createdAt()).isNotNull();
    }

    @Test
    void shouldTreatRepeatedWalletCreationAsIdempotent() {
        UUID accountId = UUID.randomUUID();
        LedgerAccount wallet = createWallet(accountId, Currency.EUR);

        LedgerAccount first = tigerBeetleAccountRepository.persist(wallet);
        LedgerAccount second = tigerBeetleAccountRepository.persist(wallet);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.createdAt()).isEqualTo(first.createdAt());
    }

    @Test
    void shouldPersistWalletsPerCurrencyForTheSameAccount() {
        UUID accountId = UUID.randomUUID();

        List<LedgerAccount> persisted = tigerBeetleAccountRepository.persistAll(List.of(
                createWallet(accountId, Currency.GBP),
                createWallet(accountId, Currency.USD),
                createWallet(accountId, Currency.JPY)));

        assertThat(persisted).hasSize(3);
        assertThat(persisted).extracting(LedgerAccount::currency)
                .containsExactlyInAnyOrder(Currency.GBP, Currency.USD, Currency.JPY);
        assertThat(persisted).extracting(LedgerAccount::id).doesNotHaveDuplicates();
    }

    @Test
    void shouldReturnEmptyWhenWalletDoesNotExist() {
        assertThat(tigerBeetleAccountRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoIdsRequested() {
        assertThat(tigerBeetleAccountRepository.findAll(List.of())).isEmpty();
    }
}
