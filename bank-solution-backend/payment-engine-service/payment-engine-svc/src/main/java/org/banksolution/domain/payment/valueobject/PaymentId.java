package org.banksolution.domain.payment.valueobject;

import java.io.Serializable;
import java.util.UUID;

public class PaymentId extends AbstractId implements Serializable {

    public PaymentId(UUID identifier) {
        super(identifier);
    }

    public PaymentId() {
        super();
    }
}
