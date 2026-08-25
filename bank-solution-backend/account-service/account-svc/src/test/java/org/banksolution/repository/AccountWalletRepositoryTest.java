package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.enums.WalletStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AccountFixtures.createAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createAccountWalletEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;

class AccountWalletRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountWalletRepository accountWalletRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        AccountEntity accountEntity = givenPersistedAccount();
        AccountWalletEntity accountWalletEntity = createAccountWalletEntity(accountEntity, Currency.GBP, true);
        accountWalletEntity.setBalance(new BigDecimal("1234.56"));
        accountWalletEntity.setAvailableBalance(new BigDecimal("1000.01"));

        UUID savedAccountWalletId = accountWalletRepository.saveAndFlush(accountWalletEntity).getId();
        AccountWalletEntity reloadedAccountWalletEntity = accountWalletRepository.findById(savedAccountWalletId).orElseThrow();

        assertThat(reloadedAccountWalletEntity.getAccount().getId()).isEqualTo(accountEntity.getId());
        assertThat(reloadedAccountWalletEntity.getLedgerAccountId()).isEqualTo(accountWalletEntity.getLedgerAccountId());
        assertThat(reloadedAccountWalletEntity.getCurrency()).isEqualTo(Currency.GBP);
        assertThat(reloadedAccountWalletEntity.getWalletStatus()).isEqualTo(WalletStatus.ACTIVE);
        assertThat(reloadedAccountWalletEntity.getBalance()).isEqualByComparingTo("1234.56");
        assertThat(reloadedAccountWalletEntity.getAvailableBalance()).isEqualByComparingTo("1000.01");
        assertThat(reloadedAccountWalletEntity.isPrimary()).isTrue();
        assertThat(reloadedAccountWalletEntity.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldAcceptEveryCurrencyAndWalletStatusAgainstTheCheckConstraints() {
        AccountEntity accountEntity = givenPersistedAccount();
        for (Currency currency : Currency.values()) {
            AccountWalletEntity accountWalletEntity = createAccountWalletEntity(accountEntity, currency, false);
            accountWalletEntity.setWalletStatus(WalletStatus.values()[currency.ordinal() % WalletStatus.values().length]);

            UUID savedAccountWalletId = accountWalletRepository.saveAndFlush(accountWalletEntity).getId();

            assertThat(accountWalletRepository.findById(savedAccountWalletId).orElseThrow().getCurrency()).isEqualTo(currency);
        }
    }

    @Test
    void shouldRejectASecondWalletInTheSameCurrencyForOneAccount() {
        AccountEntity accountEntity = givenPersistedAccount();
        accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.GBP, true));
        AccountWalletEntity duplicateAccountWalletEntity = createAccountWalletEntity(accountEntity, Currency.GBP, false);

        assertThatThrownBy(() -> accountWalletRepository.saveAndFlush(duplicateAccountWalletEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectTwoWalletsSharingOneLedgerAccount() {
        AccountEntity accountEntity = givenPersistedAccount();
        AccountWalletEntity gbpAccountWalletEntity =
                accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.GBP, true));
        AccountWalletEntity jpyAccountWalletEntity = createAccountWalletEntity(accountEntity, Currency.JPY, false);
        jpyAccountWalletEntity.setLedgerAccountId(gbpAccountWalletEntity.getLedgerAccountId());

        assertThatThrownBy(() -> accountWalletRepository.saveAndFlush(jpyAccountWalletEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectAWalletForAnAccountThatDoesNotExist() {
        AccountEntity unknownAccountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        AccountWalletEntity orphanAccountWalletEntity = createAccountWalletEntity(unknownAccountEntity, Currency.GBP, true);

        assertThatThrownBy(() -> accountWalletRepository.saveAndFlush(orphanAccountWalletEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindWalletsByAccountByCurrencyAndByLedgerAccount() {
        AccountEntity accountEntity = givenPersistedAccount();
        AccountWalletEntity gbpAccountWalletEntity =
                accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.GBP, true));
        AccountWalletEntity jpyAccountWalletEntity =
                accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.JPY, false));

        assertThat(accountWalletRepository.findByAccountId(accountEntity.getId()))
                .extracting(AccountWalletEntity::getId)
                .containsExactlyInAnyOrder(gbpAccountWalletEntity.getId(), jpyAccountWalletEntity.getId());
        assertThat(accountWalletRepository.findByAccountIdAndCurrency(accountEntity.getId(), Currency.JPY))
                .map(AccountWalletEntity::getId)
                .contains(jpyAccountWalletEntity.getId());
        assertThat(accountWalletRepository.findByAccountIdAndCurrency(accountEntity.getId(), Currency.USD)).isEmpty();
        assertThat(accountWalletRepository.findByLedgerAccountId(gbpAccountWalletEntity.getLedgerAccountId()))
                .map(AccountWalletEntity::getId)
                .contains(gbpAccountWalletEntity.getId());
    }

    private AccountEntity givenPersistedAccount() {
        return accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID()));
    }
}
