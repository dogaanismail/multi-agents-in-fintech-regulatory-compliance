package org.banksolution.repository;

import com.tigerbeetle.CreateAccountStatus;
import com.tigerbeetle.CreateTransferStatus;
import org.banksolution.enums.TransferType;

public final class TigerBeetleStatuses {

    private TigerBeetleStatuses() {
    }

    static boolean isAccountPersisted(CreateAccountStatus status) {
        return status == CreateAccountStatus.Created || status == CreateAccountStatus.Exists;
    }

    static boolean isTransferPersisted(CreateTransferStatus status, TransferType transferType) {
        return switch (status) {
            case Created, Exists -> true;
            case PendingTransferAlreadyPosted -> transferType == TransferType.POST_PENDING;
            case PendingTransferAlreadyVoided -> transferType == TransferType.VOID_PENDING;
            default -> false;
        };
    }

    static boolean isPendingTransferAlreadyResolved(CreateTransferStatus status) {
        return status == CreateTransferStatus.PendingTransferAlreadyPosted
                || status == CreateTransferStatus.PendingTransferAlreadyVoided;
    }

    static boolean isInsufficientFunds(CreateTransferStatus status) {
        return status == CreateTransferStatus.ExceedsCredits;
    }

    static boolean isPendingTransferMissing(CreateTransferStatus status) {
        return status == CreateTransferStatus.PendingTransferNotFound;
    }
}
