package org.banksolution.exception;

import java.util.UUID;

public class IdempotencyKeyReusedException extends RuntimeException {
    public IdempotencyKeyReusedException(UUID paymentId) {
        super("Idempotency-Key was already used for payment " + paymentId + " with a different request");
    }
}
