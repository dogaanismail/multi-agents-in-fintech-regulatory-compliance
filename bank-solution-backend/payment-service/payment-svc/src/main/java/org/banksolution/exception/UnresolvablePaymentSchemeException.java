package org.banksolution.exception;

import org.banksolution.enums.PaymentType;

public class UnresolvablePaymentSchemeException extends IllegalArgumentException {
    public UnresolvablePaymentSchemeException(PaymentType paymentType) {
        super("Cannot resolve a payment scheme for " + paymentType
                + ": neither a source nor a destination account was provided");
    }
}
