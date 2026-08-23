package org.banksolution.service;

import lombok.experimental.UtilityClass;
import org.banksolution.enums.PaymentScheme;
import org.banksolution.exception.UnresolvablePaymentSchemeException;
import org.banksolution.model.PaymentAccounts;
import org.banksolution.model.request.PaymentRequest;

@UtilityClass
public class PaymentSchemeClassifier {

    public static PaymentScheme classify(
            PaymentRequest request,
            PaymentAccounts resolvedAccounts) {

        if (resolvedAccounts != null) {
            return PaymentScheme.INTERNAL_TRANSFER;
        }

        if (request.getSourceAccountId() != null) {
            return PaymentScheme.EXTERNAL_OUTBOUND;
        }

        if (request.getDestinationAccountId() != null) {
            return PaymentScheme.EXTERNAL_INBOUND;
        }

        throw new UnresolvablePaymentSchemeException(request.getPaymentType());
    }
}
