package org.banksolution.domain;

public record AccountMovement(
        String counterpartyAccountId,
        double amount,
        long timestampEpochMillis) {
}
