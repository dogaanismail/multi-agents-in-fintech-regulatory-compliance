package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.BankLocation;
import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AccountFixtures.OPENING_DATE;
import static org.banksolution.fixtures.AccountFixtures.createAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createAccountWalletEntity;

class AccountRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountWalletRepository accountWalletRepository;

    @Test
    void shouldPersistAndReloadEveryColumnOfTheMigratedSchema() {
        AccountEntity accountEntity = createAccountEntity(UUID.randomUUID());
        accountEntity.setClosingDate(LocalDate.of(2027, 1, 1));

        UUID savedAccountId = accountRepository.saveAndFlush(accountEntity).getId();
        AccountEntity reloadedAccountEntity = accountRepository.findById(savedAccountId).orElseThrow();

        assertThat(reloadedAccountEntity.getCustomerId()).isEqualTo(accountEntity.getCustomerId());
        assertThat(reloadedAccountEntity.getAccountNumber()).isEqualTo(accountEntity.getAccountNumber());
        assertThat(reloadedAccountEntity.getBankLocation()).isEqualTo(BankLocation.GB);
        assertThat(reloadedAccountEntity.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(reloadedAccountEntity.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(reloadedAccountEntity.getOpeningDate()).isEqualTo(OPENING_DATE);
        assertThat(reloadedAccountEntity.getClosingDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(reloadedAccountEntity.getCreatedAt()).isNotNull();
        assertThat(reloadedAccountEntity.getUpdatedAt()).isNotNull();
        assertThat(reloadedAccountEntity.getDeletedAt()).isNull();
    }

    @Test
    void shouldAcceptEveryAccountTypeStatusAndBankLocationAgainstTheCheckConstraints() {
        for (BankLocation bankLocation : BankLocation.values()) {
            AccountEntity accountEntity = createAccountEntity(UUID.randomUUID());
            accountEntity.setBankLocation(bankLocation);
            accountRepository.saveAndFlush(accountEntity);
        }
        for (AccountType accountType : AccountType.values()) {
            for (AccountStatus accountStatus : AccountStatus.values()) {
                AccountEntity accountEntity = createAccountEntity(UUID.randomUUID());
                accountEntity.setAccountType(accountType);
                accountEntity.setAccountStatus(accountStatus);

                UUID savedAccountId = accountRepository.saveAndFlush(accountEntity).getId();
                AccountEntity reloadedAccountEntity = accountRepository.findById(savedAccountId).orElseThrow();

                assertThat(reloadedAccountEntity.getAccountType()).isEqualTo(accountType);
                assertThat(reloadedAccountEntity.getAccountStatus()).isEqualTo(accountStatus);
            }
        }
    }

    @Test
    void shouldRejectASecondAccountWithTheSameAccountNumber() {
        AccountEntity accountEntity = accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID()));
        AccountEntity duplicateAccountEntity = createAccountEntity(UUID.randomUUID());
        duplicateAccountEntity.setAccountNumber(accountEntity.getAccountNumber());

        assertThatThrownBy(() -> accountRepository.saveAndFlush(duplicateAccountEntity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldOnlyReportExistingAccountNumbers() {
        AccountEntity accountEntity = accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID()));

        assertThat(accountRepository.existsByAccountNumber(accountEntity.getAccountNumber())).isTrue();
        assertThat(accountRepository.existsByAccountNumber("0000000000")).isFalse();
    }

    @Test
    void shouldBumpTheOptimisticLockVersionWhenTheAccountIsUpdated() {
        UUID savedAccountId = accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID())).getId();

        AccountEntity persistedAccountEntity = accountRepository.findById(savedAccountId).orElseThrow();
        short versionBeforeUpdate = persistedAccountEntity.getVersion();
        persistedAccountEntity.setAccountStatus(AccountStatus.SUSPENDED);
        accountRepository.saveAndFlush(persistedAccountEntity);

        AccountEntity updatedAccountEntity = accountRepository.findById(savedAccountId).orElseThrow();
        assertThat(updatedAccountEntity.getVersion()).isEqualTo((short) (versionBeforeUpdate + 1));
        assertThat(updatedAccountEntity.getUpdatedAt()).isAfterOrEqualTo(updatedAccountEntity.getCreatedAt());
    }

    @Test
    void shouldPersistTheSoftDeletionTimestampAndHideTheAccountFromActiveLookups() {
        UUID customerId = UUID.randomUUID();
        AccountEntity accountEntity = accountRepository.saveAndFlush(createAccountEntity(customerId));

        AccountEntity persistedAccountEntity = accountRepository.findById(accountEntity.getId()).orElseThrow();
        persistedAccountEntity.setDeletedAt(Instant.now());
        persistedAccountEntity.setDeletedReason("Closed by customer");
        accountRepository.saveAndFlush(persistedAccountEntity);

        AccountEntity softDeletedAccountEntity = accountRepository.findById(accountEntity.getId()).orElseThrow();
        assertThat(softDeletedAccountEntity.getDeletedAt()).isNotNull();
        assertThat(softDeletedAccountEntity.getDeletedReason()).isEqualTo("Closed by customer");
        assertThat(accountRepository.findActiveById(accountEntity.getId())).isEmpty();
        assertThat(accountRepository.findActiveByIdIn(List.of(accountEntity.getId()))).isEmpty();
        assertThat(accountRepository.findActiveByCustomerId(customerId)).isEmpty();
    }

    @Test
    void shouldLoadTheWalletsTogetherWithTheActiveAccount() {
        AccountEntity accountEntity = accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID()));
        accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.GBP, true));
        accountWalletRepository.saveAndFlush(createAccountWalletEntity(accountEntity, Currency.JPY, false));

        AccountEntity activeAccountEntity = accountRepository.findActiveById(accountEntity.getId()).orElseThrow();

        assertThat(activeAccountEntity.getWallets())
                .extracting(AccountWalletEntity::getCurrency)
                .containsExactlyInAnyOrder(Currency.GBP, Currency.JPY);
    }

    @Test
    void shouldFindActiveAccountsByIdsAndByCustomer() {
        UUID customerId = UUID.randomUUID();
        AccountEntity firstAccountEntity = accountRepository.saveAndFlush(createAccountEntity(customerId));
        AccountEntity secondAccountEntity = accountRepository.saveAndFlush(createAccountEntity(customerId));
        AccountEntity otherCustomerAccountEntity = accountRepository.saveAndFlush(createAccountEntity(UUID.randomUUID()));

        assertThat(accountRepository.findActiveByIdIn(List.of(firstAccountEntity.getId(), otherCustomerAccountEntity.getId())))
                .extracting(AccountEntity::getId)
                .containsExactlyInAnyOrder(firstAccountEntity.getId(), otherCustomerAccountEntity.getId());
        assertThat(accountRepository.findActiveByCustomerId(customerId))
                .extracting(AccountEntity::getId)
                .containsExactlyInAnyOrder(firstAccountEntity.getId(), secondAccountEntity.getId());
    }
}
