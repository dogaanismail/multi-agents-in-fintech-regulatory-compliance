package org.banksolution.exception;

import org.banksolution.enums.Currency;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(UUID accountId, Currency currency) {
        super("Wallet not found for account: " + accountId + " and currency: " + currency);
    }
}
