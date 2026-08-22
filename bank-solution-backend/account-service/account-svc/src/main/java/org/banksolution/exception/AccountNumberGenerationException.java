package org.banksolution.exception;

public class AccountNumberGenerationException extends RuntimeException {
    public AccountNumberGenerationException(int attempts) {
        super("Could not generate a unique account number after " + attempts + " attempts");
    }
}
