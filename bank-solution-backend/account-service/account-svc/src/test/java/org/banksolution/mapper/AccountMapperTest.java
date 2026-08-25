package org.banksolution.mapper;

import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.BankLocation;
import org.banksolution.enums.Currency;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.model.response.AccountWalletResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.banksolution.fixtures.AccountFixtures.OPENING_DATE;
import static org.banksolution.fixtures.AccountFixtures.createOpenAccountRequest;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountWalletEntity;

class AccountMapperTest {

    @Test
    void shouldBuildANewActiveCheckingAccountFromTheOpenRequest() {
        UUID customerId = UUID.randomUUID();
        OpenAccountRequest openAccountRequest = createOpenAccountRequest(customerId, List.of(Currency.GBP));

        AccountEntity accountEntity = AccountMapper.toAccountEntity(openAccountRequest, "1234567890", OPENING_DATE);

        assertThat(accountEntity.getId()).isNull();
        assertThat(accountEntity.getCustomerId()).isEqualTo(customerId);
        assertThat(accountEntity.getAccountNumber()).isEqualTo("1234567890");
        assertThat(accountEntity.getBankLocation()).isEqualTo(BankLocation.GB);
        assertThat(accountEntity.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(accountEntity.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(accountEntity.getOpeningDate()).isEqualTo(OPENING_DATE);
        assertThat(accountEntity.getClosingDate()).isNull();
        assertThat(accountEntity.getWallets()).isEmpty();
    }

    @Test
    void shouldCopyEveryAccountFieldAndTheGivenWalletsIntoTheResponse() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        accountEntity.setClosingDate(LocalDate.of(2027, 1, 1));
        accountEntity.setCreatedAt(Instant.parse("2026-08-26T10:00:00Z"));
        accountEntity.setUpdatedAt(Instant.parse("2026-08-26T11:00:00Z"));
        AccountWalletEntity gbpAccountWalletEntity = createPersistedAccountWalletEntity(accountEntity, Currency.GBP, true);
        AccountWalletEntity jpyAccountWalletEntity = createPersistedAccountWalletEntity(accountEntity, Currency.JPY, false);

        AccountResponse accountResponse = AccountMapper.toAccountResponse(
                accountEntity, List.of(gbpAccountWalletEntity, jpyAccountWalletEntity));

        assertThat(accountResponse.getId()).isEqualTo(accountEntity.getId());
        assertThat(accountResponse.getCustomerId()).isEqualTo(accountEntity.getCustomerId());
        assertThat(accountResponse.getAccountNumber()).isEqualTo(accountEntity.getAccountNumber());
        assertThat(accountResponse.getBankLocation()).isEqualTo("GB");
        assertThat(accountResponse.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(accountResponse.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(accountResponse.getOpeningDate()).isEqualTo(OPENING_DATE);
        assertThat(accountResponse.getClosingDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(accountResponse.getCreatedAt()).isEqualTo(accountEntity.getCreatedAt());
        assertThat(accountResponse.getUpdatedAt()).isEqualTo(accountEntity.getUpdatedAt());
        assertThat(accountResponse.getWallets())
                .extracting(AccountWalletResponse::getId, AccountWalletResponse::getCurrency, AccountWalletResponse::isPrimary)
                .containsExactly(
                        tuple(gbpAccountWalletEntity.getId(), Currency.GBP, true),
                        tuple(jpyAccountWalletEntity.getId(), Currency.JPY, false));
    }

    @Test
    void shouldReturnAnEmptyWalletListWhenTheAccountHasNoWallets() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());

        AccountResponse accountResponse = AccountMapper.toAccountResponse(accountEntity, List.of());

        assertThat(accountResponse.getWallets()).isEmpty();
    }
}
