package org.banksolution.exception;

import java.util.UUID;

public class WalletCreationFailedException extends RuntimeException {
    public WalletCreationFailedException(UUID accountId, Throwable cause) {
        super("Could not open ledger wallets for account: " + accountId, cause);
    }
}
