package org.banksolution.repository;

import com.tigerbeetle.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.exception.InsufficientLedgerFundsException;
import org.banksolution.exception.LedgerPostingException;
import org.banksolution.exception.LedgerUnavailableException;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.banksolution.mapper.LedgerTransferMapper;
import org.banksolution.util.MoneyUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TigerBeetleTransferRepository {

    private static final int QUERY_LIMIT = 100;

    private final Client tigerBeetleClient;

    public LedgerTransfer persistLedgerTransfer(LedgerTransfer ledgerTransfer) {
        return persistLinkedLedgerTransfers(List.of(ledgerTransfer)).getFirst();
    }

    public List<LedgerTransfer> persistLinkedLedgerTransfers(List<LedgerTransfer> ledgerTransfers) {
        TransferBatch transferBatch = new TransferBatch(ledgerTransfers.size());

        for (int index = 0; index < ledgerTransfers.size(); index++) {
            boolean isLastLeg = index == ledgerTransfers.size() - 1;
            appendTransfer(transferBatch, ledgerTransfers.get(index), isLastLeg);
        }

        createTransfersInTigerBeetle(transferBatch, ledgerTransfers.getFirst());

        return findLedgerTransfersByIds(ledgerTransfers.stream().map(LedgerTransfer::id).toList());
    }

    public Optional<LedgerTransfer> findLedgerTransferById(UUID transferId) {
        TransferBatch transferBatch = lookupTransfersInTigerBeetle(new IdBatch(UInt128.asBytes(transferId)));

        return transferBatch.next()
                ? Optional.of(LedgerTransferMapper.toLedgerTransfer(transferBatch))
                : Optional.empty();
    }

    public List<LedgerTransfer> findLedgerTransfersByIds(List<UUID> transferIds) {
        if (transferIds.isEmpty()) {
            return List.of();
        }

        IdBatch idBatch = new IdBatch(transferIds.size());
        transferIds.forEach(transferId -> idBatch.add(UInt128.asBytes(transferId)));

        return toLedgerTransfers(lookupTransfersInTigerBeetle(idBatch));
    }

    public List<LedgerTransfer> findLedgerTransfersByClientTransactionId(UUID clientTransactionId) {
        QueryFilter queryFilter = new QueryFilter();
        queryFilter.setUserData128(UInt128.asBytes(clientTransactionId));
        queryFilter.setLimit(QUERY_LIMIT);

        try {
            return toLedgerTransfers(tigerBeetleClient.queryTransfers(queryFilter));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(e);
        }
    }

    private static void appendTransfer(
            TransferBatch transferBatch,
            LedgerTransfer ledgerTransfer,
            boolean isLastLeg) {

        transferBatch.add();
        transferBatch.setId(UInt128.asBytes(ledgerTransfer.id()));
        transferBatch.setUserData128(UInt128.asBytes(ledgerTransfer.clientTransactionId()));
        transferBatch.setUserData32(ledgerTransfer.postingInstructionType().getCode());

        switch (ledgerTransfer.transferType()) {
            case PENDING -> {
                applyDoubleEntryAccountsAndAmount(transferBatch, ledgerTransfer);
                transferBatch.setTimeout(ledgerTransfer.timeoutSeconds());
                transferBatch.setFlags(TransferFlags.PENDING);
            }
            case SINGLE_PHASE -> {
                applyDoubleEntryAccountsAndAmount(transferBatch, ledgerTransfer);
                transferBatch.setFlags(TransferFlags.NONE);
            }
            // Accounts, currency and code are inherited from the authorisation being resolved.
            case POST_PENDING -> {
                transferBatch.setPendingId(UInt128.asBytes(ledgerTransfer.pendingTransferId()));
                transferBatch.setAmount(TransferBatch.AMOUNT_MAX);
                transferBatch.setFlags(TransferFlags.POST_PENDING_TRANSFER);
            }
            case VOID_PENDING -> {
                transferBatch.setPendingId(UInt128.asBytes(ledgerTransfer.pendingTransferId()));
                transferBatch.setFlags(TransferFlags.VOID_PENDING_TRANSFER);
            }
        }

        if (!isLastLeg) {
            transferBatch.setFlags(transferBatch.getFlags() | TransferFlags.LINKED);
        }
    }

    private static void applyDoubleEntryAccountsAndAmount(TransferBatch transferBatch, LedgerTransfer ledgerTransfer) {
        transferBatch.setDebitAccountId(UInt128.asBytes(ledgerTransfer.debitAccountId()));
        transferBatch.setCreditAccountId(UInt128.asBytes(ledgerTransfer.creditAccountId()));
        transferBatch.setAmount(MoneyUtils.toMinorUnits(ledgerTransfer.amount(), ledgerTransfer.currency()));
        transferBatch.setLedger(ledgerTransfer.currency().getNumericCode());
        transferBatch.setCode(ledgerTransfer.postingInstructionType().getCode());
    }

    private void createTransfersInTigerBeetle(TransferBatch transferBatch, LedgerTransfer ledgerTransfer) {
        CreateTransferResultBatch results;
        try {
            results = tigerBeetleClient.createTransfers(transferBatch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(e);
        }

        while (results.next()) {
            failUnlessTransferWasApplied(results.getStatus(), ledgerTransfer);
        }
    }

    private static void failUnlessTransferWasApplied(CreateTransferStatus status, LedgerTransfer ledgerTransfer) {
        if (TigerBeetleStatuses.isTransferPersisted(status)) {
            if (status != CreateTransferStatus.Created) {
                log.info("Posting instruction {} for client transaction {} was already applied: {}",
                        ledgerTransfer.postingInstructionType(), ledgerTransfer.clientTransactionId(), status);
            }
            return;
        }

        if (TigerBeetleStatuses.isInsufficientFunds(status)) {
            throw new InsufficientLedgerFundsException(ledgerTransfer.debitAccountId());
        }

        if (TigerBeetleStatuses.isPendingTransferMissing(status)) {
            throw new PendingAuthorisationNotFoundException(ledgerTransfer.clientTransactionId());
        }

        throw new LedgerPostingException(status.name());
    }

    private TransferBatch lookupTransfersInTigerBeetle(IdBatch idBatch) {
        try {
            return tigerBeetleClient.lookupTransfers(idBatch);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LedgerUnavailableException(e);
        }
    }

    private static List<LedgerTransfer> toLedgerTransfers(TransferBatch transferBatch) {
        List<LedgerTransfer> ledgerTransfers = new ArrayList<>();

        while (transferBatch.next()) {
            ledgerTransfers.add(LedgerTransferMapper.toLedgerTransfer(transferBatch));
        }

        return ledgerTransfers;
    }
}
