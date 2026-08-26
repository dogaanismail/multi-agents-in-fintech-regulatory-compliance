package org.banksolution.repository;

import com.tigerbeetle.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerInternalAccount;
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
public class TigerBeetleInternalAccountRepository {

    private final Client tigerBeetleClient;

    public Optional<LedgerInternalAccount> findInternalAccountById(UUID ledgerAccountId) {
        return findInternalAccountsByIds(List.of(ledgerAccountId)).stream().findFirst();
    }

    public List<LedgerInternalAccount> findInternalAccountsByIds(List<UUID> ledgerAccountIds) {
        if (ledgerAccountIds.isEmpty()) {
            return List.of();
        }

        IdBatch ids = new IdBatch(ledgerAccountIds.size());
        ledgerAccountIds.forEach(ledgerAccountId -> ids.add(UInt128.asBytes(ledgerAccountId)));

        AccountBatch accounts = lookupAccountsInTigerBeetle(ids);
        List<LedgerInternalAccount> internalAccounts = new ArrayList<>();
        while (accounts.next()) {
            internalAccounts.add(LedgerAccountMapper.toLedgerInternalAccount(accounts));
        }

        return internalAccounts;
    }

    public List<LedgerInternalAccount> persistInternalAccounts(List<LedgerInternalAccount> internalAccounts) {
        if (internalAccounts.isEmpty()) {
            return List.of();
        }

        AccountBatch batch = new AccountBatch(internalAccounts.size());
        internalAccounts.forEach(internalAccount -> {
            batch.add();
            batch.setId(UInt128.asBytes(internalAccount.id()));
            batch.setLedger(internalAccount.currency().getNumericCode());
            batch.setCode(internalAccount.accountType().getCode());
            batch.setFlags(AccountFlags.HISTORY);
        });

        createAccountsInTigerBeetle(batch);

        return findInternalAccountsByIds(internalAccounts.stream().map(LedgerInternalAccount::id).toList());
    }

    public LedgerInternalAccount persistInternalAccount(LedgerInternalAccount internalAccount) {
        return persistInternalAccounts(List.of(internalAccount)).getFirst();
    }

    private AccountBatch lookupAccountsInTigerBeetle(IdBatch ids) {
        try {
            return tigerBeetleClient.lookupAccounts(ids);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(interruption);
        }
    }

    private void createAccountsInTigerBeetle(AccountBatch batch) {
        CreateAccountResultBatch results;
        try {
            results = tigerBeetleClient.createAccounts(batch);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(interruption);
        }

        while (results.next()) {
            CreateAccountStatus status = results.getStatus();
            if (!TigerBeetleStatuses.isAccountPersisted(status)) {
                throw new LedgerAccountPersistenceException(status.name());
            }
        }
    }
}
