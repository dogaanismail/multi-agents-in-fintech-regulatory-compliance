package org.banksolution.repository;

import com.tigerbeetle.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.exception.LedgerAccountPersistenceException;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.mapper.LedgerAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TigerBeetleAccountRepository {

    private static final int WALLET_FLAGS =
            AccountFlags.HISTORY | AccountFlags.DEBITS_MUST_NOT_EXCEED_CREDITS;

    private final Client tigerBeetleClient;

    public Optional<LedgerAccount> findLedgerAccountById(UUID ledgerAccountId) {
        return findLedgerAccountsByIds(List.of(ledgerAccountId)).stream().findFirst();
    }

    public List<LedgerAccount> findLedgerAccountsByIds(List<UUID> ledgerAccountIds) {
        if (ledgerAccountIds.isEmpty()) {
            return List.of();
        }

        IdBatch ids = new IdBatch(ledgerAccountIds.size());
        ledgerAccountIds.forEach(id -> ids.add(UInt128.asBytes(id)));

        AccountBatch accounts = lookupAccountsInTigerBeetle(ids);
        List<LedgerAccount> ledgerAccounts = new ArrayList<>();
        while (accounts.next()) {
            ledgerAccounts.add(LedgerAccountMapper.toLedgerAccount(accounts));
        }

        return ledgerAccounts;
    }

    public List<LedgerAccount> persistLedgerAccounts(List<LedgerAccount> ledgerAccounts) {
        if (ledgerAccounts.isEmpty()) {
            return List.of();
        }

        AccountBatch batch = new AccountBatch(ledgerAccounts.size());
        ledgerAccounts.forEach(account -> {
            batch.add();
            batch.setId(UInt128.asBytes(account.id()));
            batch.setUserData128(UInt128.asBytes(account.accountId()));
            batch.setLedger(account.currency().getNumericCode());
            batch.setCode(account.accountType().getCode());
            batch.setFlags(WALLET_FLAGS);
        });

        createAccountsInTigerBeetle(batch);

        return findLedgerAccountsByIds(ledgerAccounts.stream().map(LedgerAccount::id).toList());
    }

    public LedgerAccount persistLedgerAccount(LedgerAccount ledgerAccount) {
        return persistLedgerAccounts(List.of(ledgerAccount)).getFirst();
    }

    private AccountBatch lookupAccountsInTigerBeetle(IdBatch ids) {
        try {
            return tigerBeetleClient.lookupAccounts(ids);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(e);
        }
    }

    private void createAccountsInTigerBeetle(AccountBatch batch) {
        CreateAccountResultBatch results;
        try {
            results = tigerBeetleClient.createAccounts(batch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(e);
        }

        while (results.next()) {
            CreateAccountStatus status = results.getStatus();
            if (!TigerBeetleStatuses.isAccountPersisted(status)) {
                throw new LedgerAccountPersistenceException(status.name());
            }
        }
    }
}
