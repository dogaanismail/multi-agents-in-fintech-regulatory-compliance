package org.banksolution.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String referenceNumber) {
        super("Payment not found with reference number: " + referenceNumber);
    }
}

