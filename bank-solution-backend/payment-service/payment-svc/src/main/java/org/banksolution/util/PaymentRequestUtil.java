package org.banksolution.util;

import lombok.experimental.UtilityClass;
import org.banksolution.model.request.PaymentRequestRequest;

@UtilityClass
public class PaymentRequestUtil {

    public void validatePaymentRequest(PaymentRequestRequest request) {
        switch (request.getPaymentType()) {
            case TRANSFER_OUT, WITHDRAWAL -> requireSourceAccount(request);
            case TRANSFER_IN, DEPOSIT -> requireDestinationAccount(request);
        }
    }

    private void requireSourceAccount(PaymentRequestRequest request) {
        if (request.getSourceAccountId() == null) {
            throw new IllegalArgumentException(
                    "Source account is required for " + request.getPaymentType());
        }
    }

    private void requireDestinationAccount(PaymentRequestRequest request) {
        if (request.getDestinationAccountId() == null) {
            throw new IllegalArgumentException(
                    "Destination account is required for " + request.getPaymentType());
        }
    }
}
