package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.enums.Currency;
import org.banksolution.exception.LedgerAccountNotFoundException;
import org.banksolution.repository.TigerBeetleAccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerAccountService {

    private final TigerBeetleAccountRepository tigerBeetleAccountRepository;

    public LedgerAccount createLedgerAccount(UUID accountId, Currency currency) {
        log.info("Creating ledger account for accountId: {}, currency: {}", accountId, currency);
        return tigerBeetleAccountRepository.persistLedgerAccount(LedgerAccount.newWallet(accountId, currency));
    }

    public List<LedgerAccount> createLedgerAccounts(List<LedgerAccount> ledgerAccounts) {
        log.info("Creating {} ledger accounts", ledgerAccounts.size());
        return tigerBeetleAccountRepository.persistLedgerAccounts(ledgerAccounts);
    }

    public LedgerAccount getLedgerAccount(UUID ledgerAccountId) {
        log.info("Fetching ledger account: {}", ledgerAccountId);
        return tigerBeetleAccountRepository.findLedgerAccountById(ledgerAccountId)
                .orElseThrow(() -> new LedgerAccountNotFoundException(ledgerAccountId));
    }

    public LedgerAccount getWallet(UUID accountId, Currency currency) {
        return getLedgerAccount(LedgerAccountIds.deriveWalletAccountId(accountId, currency));
    }

    public List<LedgerAccount> getWallets(UUID accountId) {
        List<UUID> candidateIds = java.util.Arrays.stream(Currency.values())
                .map(currency -> LedgerAccountIds.deriveWalletAccountId(accountId, currency))
                .toList();

        return tigerBeetleAccountRepository.findLedgerAccountsByIds(candidateIds);
    }
}
