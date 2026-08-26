package org.banksolution.config;

import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.repository.TigerBeetleInternalAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChartOfAccountsSeederTest {

    @Mock
    private TigerBeetleInternalAccountRepository tigerBeetleInternalAccountRepository;

    @Test
    void shouldSeedEveryInternalAccountTypeForEveryConfiguredCurrency() {
        ChartOfAccountsSeeder chartOfAccountsSeeder = new ChartOfAccountsSeeder(
                tigerBeetleInternalAccountRepository,
                new ChartOfAccountsProperties(true, List.of(Currency.GBP, Currency.JPY)));

        chartOfAccountsSeeder.run(new DefaultApplicationArguments());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerInternalAccount>> internalAccountsCaptor = ArgumentCaptor.forClass(List.class);
        verify(tigerBeetleInternalAccountRepository).persistInternalAccounts(internalAccountsCaptor.capture());
        assertThat(internalAccountsCaptor.getValue())
                .hasSize(2 * LedgerAccountType.internalTypes().length)
                .extracting(LedgerInternalAccount::currency)
                .containsOnly(Currency.GBP, Currency.JPY);
        assertThat(internalAccountsCaptor.getValue())
                .extracting(LedgerInternalAccount::accountType)
                .doesNotContain(LedgerAccountType.WALLET);
    }

    @Test
    void shouldLetTheServiceStartEvenWhenSeedingFails() {
        when(tigerBeetleInternalAccountRepository.persistInternalAccounts(anyList()))
                .thenThrow(new LedgerUnavailableException(new InterruptedException("cluster down")));
        ChartOfAccountsSeeder chartOfAccountsSeeder = new ChartOfAccountsSeeder(
                tigerBeetleInternalAccountRepository,
                new ChartOfAccountsProperties(true, List.of(Currency.GBP)));

        assertThatCode(() -> chartOfAccountsSeeder.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }
}
