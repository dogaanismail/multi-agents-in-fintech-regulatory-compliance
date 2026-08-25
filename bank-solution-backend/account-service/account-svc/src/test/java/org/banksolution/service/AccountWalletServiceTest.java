package org.banksolution.service;

import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.exception.WalletNotFoundException;
import org.banksolution.model.response.AccountWalletResponse;
import org.banksolution.repository.AccountWalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.banksolution.fixtures.AccountFixtures.createLedgerAccountResponses;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountWalletEntity;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWalletServiceTest {

    @Mock
    private AccountWalletRepository accountWalletRepository;

    @Mock
    private LedgerClientService ledgerClientService;

    @InjectMocks
    private AccountWalletService accountWalletService;

    @Test
    void shouldOpenOneLedgerBackedWalletPerDistinctCurrency() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        List<Currency> distinctCurrencies = List.of(Currency.GBP, Currency.EUR);
        when(ledgerClientService.openLedgerWallets(accountEntity.getId(), distinctCurrencies))
                .thenReturn(createLedgerAccountResponses(accountEntity.getId(), distinctCurrencies));
        when(accountWalletRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<AccountWalletEntity> openedAccountWalletEntities = accountWalletService.openWallets(
                accountEntity, List.of(Currency.GBP, Currency.EUR, Currency.GBP));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AccountWalletEntity>> accountWalletEntitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(accountWalletRepository).saveAll(accountWalletEntitiesCaptor.capture());
        assertThat(accountWalletEntitiesCaptor.getValue()).isSameAs(openedAccountWalletEntities);
        assertThat(openedAccountWalletEntities)
                .extracting(AccountWalletEntity::getCurrency, AccountWalletEntity::isPrimary)
                .containsExactly(tuple(Currency.GBP, true), tuple(Currency.EUR, false));
        assertThat(openedAccountWalletEntities).allSatisfy(accountWalletEntity -> {
            assertThat(accountWalletEntity.getAccount()).isSameAs(accountEntity);
            assertThat(accountWalletEntity.getLedgerAccountId()).isNotNull();
        });
    }

    @Test
    void shouldReturnEveryWalletOfTheAccount() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        when(accountWalletRepository.findByAccountId(accountEntity.getId())).thenReturn(List.of(
                createPersistedAccountWalletEntity(accountEntity, Currency.GBP, true),
                createPersistedAccountWalletEntity(accountEntity, Currency.JPY, false)));

        List<AccountWalletResponse> accountWalletResponses = accountWalletService.getWalletsByAccountId(accountEntity.getId());

        assertThat(accountWalletResponses)
                .extracting(AccountWalletResponse::getCurrency, AccountWalletResponse::isPrimary)
                .containsExactly(tuple(Currency.GBP, true), tuple(Currency.JPY, false));
    }

    @Test
    void shouldReturnTheWalletForTheRequestedCurrency() {
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        AccountWalletEntity accountWalletEntity = createPersistedAccountWalletEntity(accountEntity, Currency.GBP, true);
        when(accountWalletRepository.findByAccountIdAndCurrency(accountEntity.getId(), Currency.GBP))
                .thenReturn(Optional.of(accountWalletEntity));

        AccountWalletResponse accountWalletResponse =
                accountWalletService.getWalletByCurrency(accountEntity.getId(), Currency.GBP);

        assertThat(accountWalletResponse.getId()).isEqualTo(accountWalletEntity.getId());
        assertThat(accountWalletResponse.getCurrency()).isEqualTo(Currency.GBP);
    }

    @Test
    void shouldFailWhenTheAccountHasNoWalletInTheRequestedCurrency() {
        UUID accountId = UUID.randomUUID();
        when(accountWalletRepository.findByAccountIdAndCurrency(accountId, Currency.JPY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountWalletService.getWalletByCurrency(accountId, Currency.JPY))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining(accountId.toString())
                .hasMessageContaining("JPY");
    }
}
