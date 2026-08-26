package org.banksolution.service;

import lombok.experimental.UtilityClass;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.exception.UnresolvablePaymentSchemeException;
import org.banksolution.model.PaymentAccounts;
import org.banksolution.model.request.PaymentRequest;

@UtilityClass
public class PaymentSchemeClassifier {

    public static PaymentScheme classify(
            PaymentRequest paymentRequest,
            PaymentAccounts resolvedAccounts) {

        if (resolvedAccounts != null) {
            return PaymentScheme.INTERNAL_TRANSFER;
        }

        if (paymentRequest.getSourceAccountId() != null) {
            return PaymentScheme.EXTERNAL_OUTBOUND;
        }

        if (paymentRequest.getDestinationAccountId() != null) {
            return PaymentScheme.EXTERNAL_INBOUND;
        }

        throw new UnresolvablePaymentSchemeException(paymentRequest.getPaymentType());
    }
}
