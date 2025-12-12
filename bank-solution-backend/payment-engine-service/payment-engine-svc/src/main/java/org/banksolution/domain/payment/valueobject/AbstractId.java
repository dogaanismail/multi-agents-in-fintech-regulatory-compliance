package org.banksolution.domain.payment.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

abstract class AbstractId {

    private final UUID identifier;

    protected AbstractId() {
        this.identifier = UUID.randomUUID();
    }

    @JsonCreator
    protected AbstractId(@JsonProperty("identifier") UUID identifier) {
        this.identifier = identifier;
    }

    @JsonProperty("identifier")
    public UUID getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return this.identifier.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractId that = (AbstractId) o;
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }
}