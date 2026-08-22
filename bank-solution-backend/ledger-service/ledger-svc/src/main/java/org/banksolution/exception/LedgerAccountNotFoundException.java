package org.banksolution.exception;

import java.util.UUID;

public class LedgerAccountNotFoundException extends RuntimeException {
    public LedgerAccountNotFoundException(UUID id) {
        super("Ledger account not found with id: " + id);
    }
}
