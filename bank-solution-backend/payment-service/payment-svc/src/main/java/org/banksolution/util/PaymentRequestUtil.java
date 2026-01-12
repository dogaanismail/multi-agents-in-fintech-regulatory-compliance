package org.banksolution.util;

import lombok.experimental.UtilityClass;
import org.banksolution.model.request.PaymentRequest;

@UtilityClass
public class PaymentRequestUtil {

    public void validatePaymentRequest(PaymentRequest request) {
        switch (request.getPaymentType()) {
            case TRANSFER_OUT, WITHDRAWAL -> requireSourceAccount(request);
            case TRANSFER_IN, DEPOSIT -> requireDestinationAccount(request);
        }
    }

    private void requireSourceAccount(PaymentRequest request) {
        if (request.getSourceAccountId() == null) {
            throw new IllegalArgumentException(
                    "Source account is required for " + request.getPaymentType());
        }
    }

    private void requireDestinationAccount(PaymentRequest request) {
        if (request.getDestinationAccountId() == null) {
            throw new IllegalArgumentException(
                    "Destination account is required for " + request.getPaymentType());
        }
    }
}
