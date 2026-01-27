package org.banksolution.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
