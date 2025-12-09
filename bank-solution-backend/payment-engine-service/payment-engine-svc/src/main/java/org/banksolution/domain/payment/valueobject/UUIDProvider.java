package org.banksolution.domain.payment.valueobject;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UUIDProvider {

    public static PaymentId generatePaymentId() {
        return new PaymentId();
    }
}
