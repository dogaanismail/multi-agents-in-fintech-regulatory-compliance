package org.banksolution.mapper;

import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.enums.WalletStatus;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.banksolution.model.response.AccountWalletResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.banksolution.fixtures.AccountFixtures.createLedgerAccountResponse;
import static org.banksolution.fixtures.AccountFixtures.createLedgerAccountResponses;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountWalletEntity;

class AccountWalletMapperTest {

    @Test
    void shouldOpenOneWalletPerCurrencyLinkedToItsLedgerAccount() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        List<Currency> currencies = List.of(Currency.GBP, Currency.EUR, Currency.JPY);
        List<LedgerAccountResponse> ledgerAccountResponses = createLedgerAccountResponses(accountEntity.getId(), currencies);

        List<AccountWalletEntity> accountWalletEntities =
                AccountWalletMapper.toAccountWalletEntities(accountEntity, currencies, ledgerAccountResponses);

        assertThat(accountWalletEntities)
                .extracting(AccountWalletEntity::getCurrency, AccountWalletEntity::getLedgerAccountId)
                .containsExactly(
                        tuple(Currency.GBP, ledgerAccountResponses.get(0).getLedgerAccountId()),
                        tuple(Currency.EUR, ledgerAccountResponses.get(1).getLedgerAccountId()),
                        tuple(Currency.JPY, ledgerAccountResponses.get(2).getLedgerAccountId()));
        assertThat(accountWalletEntities).allSatisfy(accountWalletEntity -> {
            assertThat(accountWalletEntity.getAccount()).isSameAs(accountEntity);
            assertThat(accountWalletEntity.getWalletStatus()).isEqualTo(WalletStatus.ACTIVE);
            assertThat(accountWalletEntity.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(accountWalletEntity.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void shouldMarkOnlyTheFirstRequestedCurrencyAsThePrimaryWallet() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        List<Currency> currencies = List.of(Currency.JPY, Currency.GBP);

        List<AccountWalletEntity> accountWalletEntities = AccountWalletMapper.toAccountWalletEntities(
                accountEntity, currencies, createLedgerAccountResponses(accountEntity.getId(), currencies));

        assertThat(accountWalletEntities)
                .extracting(AccountWalletEntity::getCurrency, AccountWalletEntity::isPrimary)
                .containsExactly(tuple(Currency.JPY, true), tuple(Currency.GBP, false));
    }

    @Test
    void shouldKeepTheFirstLedgerAccountWhenTheLedgerAnswersTheSameCurrencyTwice() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        LedgerAccountResponse firstGbpLedgerAccountResponse = createLedgerAccountResponse(accountEntity.getId(), Currency.GBP);
        LedgerAccountResponse duplicateGbpLedgerAccountResponse = createLedgerAccountResponse(accountEntity.getId(), Currency.GBP);
        List<LedgerAccountResponse> ledgerAccountResponses = new ArrayList<>();
        ledgerAccountResponses.add(firstGbpLedgerAccountResponse);
        ledgerAccountResponses.add(duplicateGbpLedgerAccountResponse);

        List<AccountWalletEntity> accountWalletEntities = AccountWalletMapper.toAccountWalletEntities(
                accountEntity, List.of(Currency.GBP), ledgerAccountResponses);

        assertThat(accountWalletEntities)
                .extracting(AccountWalletEntity::getLedgerAccountId)
                .containsExactly(firstGbpLedgerAccountResponse.getLedgerAccountId());
    }

    @Test
    void shouldCopyEveryWalletFieldIntoTheResponse() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        AccountWalletEntity accountWalletEntity = createPersistedAccountWalletEntity(accountEntity, Currency.GBP, true);
        accountWalletEntity.setWalletStatus(WalletStatus.SUSPENDED);
        accountWalletEntity.setBalance(new BigDecimal("750.00"));
        accountWalletEntity.setAvailableBalance(new BigDecimal("650.00"));

        AccountWalletResponse accountWalletResponse = AccountWalletMapper.toAccountWalletResponse(accountWalletEntity);

        assertThat(accountWalletResponse.getId()).isEqualTo(accountWalletEntity.getId());
        assertThat(accountWalletResponse.getLedgerAccountId()).isEqualTo(accountWalletEntity.getLedgerAccountId());
        assertThat(accountWalletResponse.getCurrency()).isEqualTo(Currency.GBP);
        assertThat(accountWalletResponse.getWalletStatus()).isEqualTo(WalletStatus.SUSPENDED);
        assertThat(accountWalletResponse.getBalance()).isEqualByComparingTo("750.00");
        assertThat(accountWalletResponse.getAvailableBalance()).isEqualByComparingTo("650.00");
        assertThat(accountWalletResponse.isPrimary()).isTrue();
    }
}
