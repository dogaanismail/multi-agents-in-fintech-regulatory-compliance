package org.banksolution.exception;

public class WalletBalanceChangedEventException extends RuntimeException {

    public WalletBalanceChangedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }
}
