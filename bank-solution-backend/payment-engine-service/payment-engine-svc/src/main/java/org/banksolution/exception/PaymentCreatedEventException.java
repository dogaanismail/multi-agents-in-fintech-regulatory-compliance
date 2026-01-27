package org.banksolution.exception;

public class PaymentCreatedEventException extends RuntimeException {

    public PaymentCreatedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
