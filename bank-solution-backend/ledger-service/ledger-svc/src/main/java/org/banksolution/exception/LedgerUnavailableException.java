package org.banksolution.exception;

public class LedgerUnavailableException extends RuntimeException {
    public LedgerUnavailableException(Throwable cause) {
        super("Ledger is unavailable: " + cause.getMessage(), cause);
    }
}
