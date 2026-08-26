package org.banksolution.util;

import lombok.experimental.UtilityClass;
import org.banksolution.model.request.PaymentRequest;

@UtilityClass
public class PaymentRequestUtil {

    public void validatePaymentRequest(PaymentRequest paymentRequest) {
        switch (paymentRequest.getPaymentType()) {
            case TRANSFER_OUT, WITHDRAWAL -> requireSourceAccount(paymentRequest);
            default -> requireDestinationAccount(paymentRequest);
        }
    }

    private void requireSourceAccount(PaymentRequest paymentRequest) {
        if (paymentRequest.getSourceAccountId() == null) {
            throw new IllegalArgumentException(
                    "Source account is required for " + paymentRequest.getPaymentType());
        }
    }

    private void requireDestinationAccount(PaymentRequest paymentRequest) {
        if (paymentRequest.getDestinationAccountId() == null) {
            throw new IllegalArgumentException(
                    "Destination account is required for " + paymentRequest.getPaymentType());
        }
    }
}
