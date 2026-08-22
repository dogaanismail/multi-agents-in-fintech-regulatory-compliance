package org.banksolution.infrastructure.tigerbeetle;

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

    public Optional<LedgerInternalAccount> findById(UUID ledgerAccountId) {
        return findAll(List.of(ledgerAccountId)).stream().findFirst();
    }

    public List<LedgerInternalAccount> findAll(List<UUID> ledgerAccountIds) {
        if (ledgerAccountIds.isEmpty()) {
            return List.of();
        }

        IdBatch ids = new IdBatch(ledgerAccountIds.size());
        ledgerAccountIds.forEach(id -> ids.add(UInt128.asBytes(id)));

        AccountBatch accounts = lookup(ids);
        List<LedgerInternalAccount> internalAccounts = new ArrayList<>();
        while (accounts.next()) {
            internalAccounts.add(LedgerAccountMapper.toLedgerInternalAccount(accounts));
        }
        return internalAccounts;
    }

    public List<LedgerInternalAccount> persistAll(List<LedgerInternalAccount> internalAccounts) {
        if (internalAccounts.isEmpty()) {
            return List.of();
        }

        AccountBatch batch = new AccountBatch(internalAccounts.size());
        internalAccounts.forEach(account -> {
            batch.add();
            batch.setId(UInt128.asBytes(account.id()));
            batch.setLedger(account.currency().getNumericCode());
            batch.setCode(account.accountType().getCode());
            batch.setFlags(AccountFlags.HISTORY);
        });

        create(batch);

        return findAll(internalAccounts.stream().map(LedgerInternalAccount::id).toList());
    }

    public LedgerInternalAccount persist(LedgerInternalAccount internalAccount) {
        return persistAll(List.of(internalAccount)).getFirst();
    }

    private AccountBatch lookup(IdBatch ids) {
        try {
            return tigerBeetleClient.lookupAccounts(ids);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(e);
        }
    }

    private void create(AccountBatch batch) {
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
