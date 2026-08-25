package org.banksolution.service;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.repository.AccountWalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createPersistedAccountWalletEntity;
import static org.banksolution.fixtures.AccountFixtures.createWalletBalanceChangedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWalletBalanceServiceTest {

    @Mock
    private AccountWalletRepository accountWalletRepository;

    @InjectMocks
    private AccountWalletBalanceService accountWalletBalanceService;

    @Test
    void shouldOverwriteTheProjectedBalancesWithTheLedgerAbsolutes() {
        AccountWalletEntity accountWalletEntity = createGbpAccountWalletEntity();
        when(accountWalletRepository.findByLedgerAccountId(accountWalletEntity.getLedgerAccountId()))
                .thenReturn(Optional.of(accountWalletEntity));

        accountWalletBalanceService.applyWalletBalanceChange(
                createWalletBalanceChangedEvent(accountWalletEntity.getLedgerAccountId().toString(), "750.00", "650.00"));

        assertThat(accountWalletEntity.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(accountWalletEntity.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("650.00"));
        verify(accountWalletRepository).save(accountWalletEntity);
    }

    @Test
    void shouldLandOnTheSameBalanceWhenTheSameEventIsRedelivered() {
        AccountWalletEntity accountWalletEntity = createGbpAccountWalletEntity();
        when(accountWalletRepository.findByLedgerAccountId(accountWalletEntity.getLedgerAccountId()))
                .thenReturn(Optional.of(accountWalletEntity));
        WalletBalanceChangedEvent walletBalanceChangedEvent =
                createWalletBalanceChangedEvent(accountWalletEntity.getLedgerAccountId().toString(), "750.00", "650.00");

        accountWalletBalanceService.applyWalletBalanceChange(walletBalanceChangedEvent);
        accountWalletBalanceService.applyWalletBalanceChange(walletBalanceChangedEvent);

        assertThat(accountWalletEntity.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(accountWalletEntity.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("650.00"));
    }

    @Test
    void shouldIgnoreBalanceChangesForUnknownWallets() {
        UUID unknownLedgerAccountId = UUID.randomUUID();
        when(accountWalletRepository.findByLedgerAccountId(unknownLedgerAccountId)).thenReturn(Optional.empty());

        accountWalletBalanceService.applyWalletBalanceChange(
                createWalletBalanceChangedEvent(unknownLedgerAccountId.toString(), "750.00", "650.00"));

        verify(accountWalletRepository, never()).save(any());
    }

    @Test
    void shouldRejectAnEventWhoseLedgerAccountIdIsNotAUuid() {
        WalletBalanceChangedEvent malformedWalletBalanceChangedEvent =
                createWalletBalanceChangedEvent("not-a-uuid", "750.00", "650.00");

        assertThatThrownBy(() -> accountWalletBalanceService.applyWalletBalanceChange(malformedWalletBalanceChangedEvent))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(accountWalletRepository);
    }

    private static AccountWalletEntity createGbpAccountWalletEntity() {
        return createPersistedAccountWalletEntity(
                createPersistedAccountEntity(UUID.randomUUID(), UUID.randomUUID()), Currency.GBP, true);
    }
}
