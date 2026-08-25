package org.banksolution.service;

import com.aml.ledger.WalletBalanceChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.repository.AccountWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountWalletBalanceService {

    private final AccountWalletRepository accountWalletRepository;

    @Transactional
    public void applyWalletBalanceChange(WalletBalanceChangedEvent walletBalanceChangedEvent) {
        UUID ledgerAccountId = UUID.fromString(walletBalanceChangedEvent.getLedgerAccountId());

        Optional<AccountWalletEntity> optionalAccountWalletEntity =
                accountWalletRepository.findByLedgerAccountId(ledgerAccountId);

        if (optionalAccountWalletEntity.isEmpty()) {
            log.warn("No wallet found for ledgerAccountId: {}, ignoring balance change", ledgerAccountId);
            return;
        }

        AccountWalletEntity accountWalletEntity = optionalAccountWalletEntity.get();
        accountWalletEntity.setBalance(new BigDecimal(walletBalanceChangedEvent.getPostedBalance()));
        accountWalletEntity.setAvailableBalance(new BigDecimal(walletBalanceChangedEvent.getAvailableBalance()));
        accountWalletRepository.save(accountWalletEntity);

        log.info("Projected ledger balances onto wallet {}: balance:{}, availableBalance:{}",
                accountWalletEntity.getId(),
                walletBalanceChangedEvent.getPostedBalance(),
                walletBalanceChangedEvent.getAvailableBalance());
    }
}
