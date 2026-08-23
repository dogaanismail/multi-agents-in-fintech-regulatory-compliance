package org.banksolution.mapper;

import com.aml.ledger.WalletBalanceChangedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.LedgerAccount;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class WalletBalanceChangedEventMapper {

    public static WalletBalanceChangedEvent toWalletBalanceChangedEvent(LedgerAccount walletAccount) {
        return WalletBalanceChangedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setLedgerAccountId(walletAccount.id().toString())
                .setCustomerAccountId(walletAccount.accountId().toString())
                .setCurrency(walletAccount.currency().name())
                .setPostedBalance(walletAccount.creditsPosted().subtract(walletAccount.debitsPosted()).toPlainString())
                .setAvailableBalance(walletAccount.availableBalance().toPlainString())
                .setPendingDebits(walletAccount.debitsPending().toPlainString())
                .setPendingCredits(walletAccount.creditsPending().toPlainString())
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }
}
