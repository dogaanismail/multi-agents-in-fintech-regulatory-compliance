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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountWalletBalanceServiceTest {

    private static final UUID LEDGER_ACCOUNT_ID = UUID.randomUUID();

    @Mock
    private AccountWalletRepository accountWalletRepository;

    @InjectMocks
    private AccountWalletBalanceService accountWalletBalanceService;

    @Test
    void shouldOverwriteTheProjectedBalancesWithTheLedgerAbsolutes() {
        AccountWalletEntity wallet = createWallet();
        when(accountWalletRepository.findByLedgerAccountId(LEDGER_ACCOUNT_ID)).thenReturn(Optional.of(wallet));

        accountWalletBalanceService.applyWalletBalanceChange(
                createWalletBalanceChangedEvent("750.00", "650.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("650.00"));
        verify(accountWalletRepository).save(wallet);
    }

    @Test
    void shouldLandOnTheSameBalanceWhenTheSameEventIsRedelivered() {
        AccountWalletEntity wallet = createWallet();
        when(accountWalletRepository.findByLedgerAccountId(LEDGER_ACCOUNT_ID)).thenReturn(Optional.of(wallet));
        WalletBalanceChangedEvent event = createWalletBalanceChangedEvent("750.00", "650.00");

        accountWalletBalanceService.applyWalletBalanceChange(event);
        accountWalletBalanceService.applyWalletBalanceChange(event);

        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("650.00"));
    }

    @Test
    void shouldIgnoreBalanceChangesForUnknownWallets() {
        when(accountWalletRepository.findByLedgerAccountId(LEDGER_ACCOUNT_ID)).thenReturn(Optional.empty());

        accountWalletBalanceService.applyWalletBalanceChange(
                createWalletBalanceChangedEvent("750.00", "650.00"));

        verify(accountWalletRepository, never()).save(any());
    }

    private static AccountWalletEntity createWallet() {
        return AccountWalletEntity.builder()
                .id(UUID.randomUUID())
                .ledgerAccountId(LEDGER_ACCOUNT_ID)
                .currency(Currency.GBP)
                .build();
    }

    private static WalletBalanceChangedEvent createWalletBalanceChangedEvent(
            String postedBalance,
            String availableBalance) {

        return WalletBalanceChangedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setLedgerAccountId(LEDGER_ACCOUNT_ID.toString())
                .setCustomerAccountId(UUID.randomUUID().toString())
                .setCurrency("GBP")
                .setPostedBalance(postedBalance)
                .setAvailableBalance(availableBalance)
                .setPendingDebits("100.00")
                .setPendingCredits("0.00")
                .setTimestamp(System.currentTimeMillis())
                .build();
    }
}
