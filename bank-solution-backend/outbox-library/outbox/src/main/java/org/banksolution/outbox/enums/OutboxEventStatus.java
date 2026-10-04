package org.banksolution.outbox.enums;

public enum OutboxEventStatus {
    PENDING,
    RETRY,
    PROCESSED,
    FAILED
}
