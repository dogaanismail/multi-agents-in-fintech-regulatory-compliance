package org.banksolution.exception;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException() {
        super("Account already exists");
    }
}

