package org.banksolution.exception;

public class LedgerAccountPersistenceException extends RuntimeException {
    public LedgerAccountPersistenceException(String status) {
        super("Failed to persist ledger account, TigerBeetle returned: " + status);
    }
}
