package org.banksolution.http;

public final class IdempotencyHeaders {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private IdempotencyHeaders() {
    }
}
