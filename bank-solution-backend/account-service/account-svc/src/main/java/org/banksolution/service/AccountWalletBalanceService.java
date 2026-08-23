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
    public void applyWalletBalanceChange(WalletBalanceChangedEvent event) {
        UUID ledgerAccountId = UUID.fromString(event.getLedgerAccountId());

        Optional<AccountWalletEntity> wallet = accountWalletRepository.findByLedgerAccountId(ledgerAccountId);

        if (wallet.isEmpty()) {
            log.warn("No wallet found for ledgerAccountId: {}, ignoring balance change", ledgerAccountId);
            return;
        }

        AccountWalletEntity accountWallet = wallet.get();
        accountWallet.setBalance(new BigDecimal(event.getPostedBalance()));
        accountWallet.setAvailableBalance(new BigDecimal(event.getAvailableBalance()));
        accountWalletRepository.save(accountWallet);

        log.info("Projected ledger balances onto wallet {}: balance:{}, availableBalance:{}",
                accountWallet.getId(),
                event.getPostedBalance(),
                event.getAvailableBalance());
    }
}
