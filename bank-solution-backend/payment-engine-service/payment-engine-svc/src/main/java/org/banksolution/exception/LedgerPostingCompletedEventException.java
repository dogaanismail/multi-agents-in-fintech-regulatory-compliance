package org.banksolution.exception;

public class LedgerPostingCompletedEventException extends RuntimeException {

    public LedgerPostingCompletedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
