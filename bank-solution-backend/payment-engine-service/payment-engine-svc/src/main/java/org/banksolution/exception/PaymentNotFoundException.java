package org.banksolution.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String identifier) {
        super("Payment not found: " + identifier);
    }
}
