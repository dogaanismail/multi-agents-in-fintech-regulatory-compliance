package org.banksolution.repository;

import com.tigerbeetle.CreateAccountStatus;
import com.tigerbeetle.CreateTransferStatus;

import java.util.EnumSet;
import java.util.Set;

public final class TigerBeetleStatuses {

    /**
     * Redelivery of the same posting instruction re-derives the same transfer id, so these
     * are the ledger telling us the work is already done rather than a failure.
     */
    private static final Set<CreateTransferStatus> ALREADY_APPLIED = EnumSet.of(
            CreateTransferStatus.Exists,
            CreateTransferStatus.PendingTransferAlreadyPosted,
            CreateTransferStatus.PendingTransferAlreadyVoided);

    private TigerBeetleStatuses() {
    }

    static boolean isAccountPersisted(CreateAccountStatus status) {
        return status == CreateAccountStatus.Created || status == CreateAccountStatus.Exists;
    }

    static boolean isTransferPersisted(CreateTransferStatus status) {
        return status == CreateTransferStatus.Created || ALREADY_APPLIED.contains(status);
    }

    static boolean isInsufficientFunds(CreateTransferStatus status) {
        return status == CreateTransferStatus.ExceedsCredits;
    }

    static boolean isPendingTransferMissing(CreateTransferStatus status) {
        return status == CreateTransferStatus.PendingTransferNotFound;
    }
}
