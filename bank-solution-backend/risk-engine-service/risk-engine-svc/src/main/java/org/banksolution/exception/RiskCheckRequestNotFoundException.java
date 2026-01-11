package org.banksolution.exception;

import java.util.UUID;

public class RiskCheckRequestNotFoundException extends RuntimeException {

    public RiskCheckRequestNotFoundException(UUID id) {
        super("Risk check request not found with id: " + id);
    }
}
