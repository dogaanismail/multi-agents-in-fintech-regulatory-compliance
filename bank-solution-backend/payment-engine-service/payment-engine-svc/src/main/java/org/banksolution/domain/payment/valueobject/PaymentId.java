package org.banksolution.domain.payment.valueobject;

import lombok.Value;
import org.axonframework.common.IdentifierFactory;

import java.io.Serializable;
import java.util.UUID;

@Value
public class PaymentId implements Serializable {
    
    String identifier;

    public PaymentId() {
        this.identifier = IdentifierFactory.getInstance().generateIdentifier();
    }

    public PaymentId(String identifier) {
        this.identifier = identifier;
    }

    public PaymentId(UUID uuid) {
        this.identifier = uuid.toString();
    }

    public UUID asUUID() {
        return UUID.fromString(identifier);
    }

    @Override
    public String toString() {
        return identifier;
    }
}
