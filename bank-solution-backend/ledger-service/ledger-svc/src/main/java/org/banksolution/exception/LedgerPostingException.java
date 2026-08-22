package org.banksolution.exception;

public class LedgerPostingException extends RuntimeException {
    public LedgerPostingException(String status) {
        super("Failed to post ledger transfer, TigerBeetle returned: " + status);
    }
}
