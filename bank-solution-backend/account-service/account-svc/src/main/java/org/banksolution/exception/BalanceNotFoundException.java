package org.banksolution.exception;

import org.banksolution.entity.enums.Currency;

import java.util.UUID;

public class BalanceNotFoundException extends RuntimeException {
    public BalanceNotFoundException(UUID accountId, Currency currency) {
        super("Balance not found for account: " + accountId + " with currency: " + currency);
    }
}

