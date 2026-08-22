package org.banksolution.exception;

import java.util.UUID;

public class PendingAuthorisationNotFoundException extends RuntimeException {
    public PendingAuthorisationNotFoundException(UUID clientTransactionId) {
        super("No authorisation found for client transaction: " + clientTransactionId);
    }
}
