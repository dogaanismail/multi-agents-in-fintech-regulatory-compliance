package org.banksolution.service;

import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.AccountNumberGenerationException;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.model.response.AccountWalletResponse;
import org.banksolution.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AccountFixtures.OPENING_DATE;
import static org.banksolution.fixtures.AccountFixtures.createOpenAccountRequest;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountWalletEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountWalletService accountWalletService;

    @Mock
    private CustomerClientService customerClientService;

    private final Clock fixedClock = Clock.fixed(
            OPENING_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    private AccountService accountService;

    private AccountService createAccountService() {
        return new AccountService(accountRepository, accountWalletService, customerClientService, fixedClock);
    }

    @Test
    void shouldOpenAnAccountWithWalletsForAnExistingCustomer() {
        accountService = createAccountService();
        UUID customerId = UUID.randomUUID();
        OpenAccountRequest openAccountRequest = createOpenAccountRequest(customerId, List.of(Currency.GBP, Currency.JPY));
        when(customerClientService.customerExists(customerId)).thenReturn(true);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        AccountEntity savedAccountEntity = createPersistedAccountEntity(UUID.randomUUID(), customerId);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(savedAccountEntity);
        AccountWalletEntity gbpAccountWalletEntity = createPersistedAccountWalletEntity(savedAccountEntity, Currency.GBP, true);
        when(accountWalletService.openWallets(savedAccountEntity, openAccountRequest.getCurrencies()))
                .thenReturn(List.of(gbpAccountWalletEntity));

        AccountResponse accountResponse = accountService.openAccount(openAccountRequest);

        ArgumentCaptor<AccountEntity> accountEntityCaptor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(accountEntityCaptor.capture());
        assertThat(accountEntityCaptor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(accountEntityCaptor.getValue().getOpeningDate()).isEqualTo(OPENING_DATE);
        assertThat(accountEntityCaptor.getValue().getAccountNumber()).matches("[1-9][0-9]{9}");
        assertThat(accountResponse.getId()).isEqualTo(savedAccountEntity.getId());
        assertThat(accountResponse.getWallets())
                .extracting(AccountWalletResponse::getCurrency)
                .containsExactly(Currency.GBP);
    }

    @Test
    void shouldRetryTheAccountNumberUntilAnUnusedOneIsFound() {
        accountService = createAccountService();
        UUID customerId = UUID.randomUUID();
        OpenAccountRequest openAccountRequest = createOpenAccountRequest(customerId, List.of(Currency.GBP));
        when(customerClientService.customerExists(customerId)).thenReturn(true);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true, true, false);
        when(accountRepository.save(any(AccountEntity.class)))
                .thenReturn(createPersistedAccountEntity(UUID.randomUUID(), customerId));

        accountService.openAccount(openAccountRequest);

        verify(accountRepository, times(3)).existsByAccountNumber(anyString());
        verify(accountRepository).save(any(AccountEntity.class));
    }

    @Test
    void shouldGiveUpWhenEveryGeneratedAccountNumberIsTaken() {
        accountService = createAccountService();
        UUID customerId = UUID.randomUUID();
        OpenAccountRequest openAccountRequest = createOpenAccountRequest(customerId, List.of(Currency.GBP));
        when(customerClientService.customerExists(customerId)).thenReturn(true);
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(true);

        assertThatThrownBy(() -> accountService.openAccount(openAccountRequest))
                .isInstanceOf(AccountNumberGenerationException.class)
                .hasMessageContaining(MAX_ACCOUNT_NUMBER_ATTEMPTS + " attempts");

        verify(accountRepository, times(MAX_ACCOUNT_NUMBER_ATTEMPTS)).existsByAccountNumber(anyString());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void shouldRefuseToOpenAnAccountForAnUnknownCustomer() {
        accountService = createAccountService();
        UUID unknownCustomerId = UUID.randomUUID();
        OpenAccountRequest openAccountRequest = createOpenAccountRequest(unknownCustomerId, List.of(Currency.GBP));
        when(customerClientService.customerExists(unknownCustomerId)).thenReturn(false);

        assertThatThrownBy(() -> accountService.openAccount(openAccountRequest))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining(unknownCustomerId.toString());

        verify(accountRepository, never()).save(any());
        verify(accountWalletService, never()).openWallets(any(), any());
    }

    @Test
    void shouldReturnTheActiveAccountWithItsWallets() {
        accountService = createAccountService();
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        accountEntity.setWallets(List.of(createPersistedAccountWalletEntity(accountEntity, Currency.EUR, true)));
        when(accountRepository.findActiveById(accountEntity.getId())).thenReturn(Optional.of(accountEntity));

        AccountResponse accountResponse = accountService.getAccountById(accountEntity.getId());

        assertThat(accountResponse.getId()).isEqualTo(accountEntity.getId());
        assertThat(accountResponse.getWallets()).extracting(AccountWalletResponse::getCurrency).containsExactly(Currency.EUR);
    }

    @Test
    void shouldFailWhenTheAccountDoesNotExistOrIsDeleted() {
        accountService = createAccountService();
        UUID unknownAccountId = UUID.randomUUID();
        when(accountRepository.findActiveById(unknownAccountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(unknownAccountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(unknownAccountId.toString());
    }

    @Test
    void shouldReturnEveryActiveAccountForTheGivenIds() {
        accountService = createAccountService();
        AccountEntity firstAccountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        AccountEntity secondAccountEntity = createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID());
        List<UUID> accountIds = List.of(firstAccountEntity.getId(), secondAccountEntity.getId());
        when(accountRepository.findActiveByIdIn(accountIds)).thenReturn(List.of(firstAccountEntity, secondAccountEntity));

        List<AccountResponse> accountResponses = accountService.getByAccountIds(accountIds);

        assertThat(accountResponses).extracting(AccountResponse::getId).containsExactlyElementsOf(accountIds);
    }

    @Test
    void shouldReturnEveryActiveAccountOfTheCustomer() {
        accountService = createAccountService();
        UUID customerId = UUID.randomUUID();
        AccountEntity accountEntity = createPersistedAccountEntity(UUID.randomUUID(), customerId);
        when(accountRepository.findActiveByCustomerId(customerId)).thenReturn(List.of(accountEntity));

        List<AccountResponse> accountResponses = accountService.getAccountsByCustomerId(customerId);

        assertThat(accountResponses).extracting(AccountResponse::getCustomerId).containsExactly(customerId);
    }
}
