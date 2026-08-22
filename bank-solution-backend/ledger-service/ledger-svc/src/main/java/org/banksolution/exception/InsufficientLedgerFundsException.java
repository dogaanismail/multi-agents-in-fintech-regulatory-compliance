package org.banksolution.exception;

import java.util.UUID;

public class InsufficientLedgerFundsException extends RuntimeException {
    public InsufficientLedgerFundsException(UUID debitAccountId) {
        super("Insufficient funds on ledger account: " + debitAccountId);
    }
}
