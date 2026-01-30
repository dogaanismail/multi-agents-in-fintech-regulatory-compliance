package org.banksolution.exception;

public class AccountChargeCompletedEventException extends RuntimeException {

    public AccountChargeCompletedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
