package org.banksolution.exception;

import org.banksolution.enums.Currency;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, Currency currency) {
        super("Insufficient funds for account: " + accountId + ", currency: " + currency);
    }
}
