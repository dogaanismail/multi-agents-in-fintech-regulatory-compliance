package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.exception.LedgerAccountNotFoundException;
import org.banksolution.infrastructure.tigerbeetle.TigerBeetleInternalAccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerInternalAccountService {

    private final TigerBeetleInternalAccountRepository tigerBeetleInternalAccountRepository;

    public LedgerInternalAccount createInternalAccount(LedgerAccountType accountType, Currency currency) {
        if (!accountType.isInternal()) {
            throw new IllegalArgumentException(accountType + " is not an internal account type");
        }

        log.info("Creating internal ledger account type: {}, currency: {}", accountType, currency);
        return tigerBeetleInternalAccountRepository.persist(LedgerInternalAccount.of(accountType, currency));
    }

    public LedgerInternalAccount getInternalAccount(UUID ledgerAccountId) {
        return tigerBeetleInternalAccountRepository.findById(ledgerAccountId)
                .orElseThrow(() -> new LedgerAccountNotFoundException(ledgerAccountId));
    }

    public LedgerInternalAccount getInternalAccount(LedgerAccountType accountType, Currency currency) {
        return getInternalAccount(LedgerAccountIds.internal(accountType, currency));
    }

    public List<LedgerInternalAccount> getInternalAccounts() {
        List<UUID> candidateIds = Arrays.stream(Currency.values())
                .flatMap(currency -> Arrays.stream(LedgerAccountType.internalTypes())
                        .map(type -> LedgerAccountIds.internal(type, currency)))
                .toList();

        return tigerBeetleInternalAccountRepository.findAll(candidateIds);
    }

    public List<LedgerInternalAccount> getInternalAccounts(Currency currency) {
        List<UUID> candidateIds = Arrays.stream(LedgerAccountType.internalTypes())
                .map(type -> LedgerAccountIds.internal(type, currency))
                .toList();

        return tigerBeetleInternalAccountRepository.findAll(candidateIds);
    }

    public BigDecimal netBalance(Currency currency) {
        return getInternalAccounts(currency).stream()
                .map(LedgerInternalAccount::netBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
