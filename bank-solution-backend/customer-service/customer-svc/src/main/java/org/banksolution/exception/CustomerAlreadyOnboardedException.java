package org.banksolution.exception;

public class CustomerAlreadyOnboardedException extends RuntimeException {

    public CustomerAlreadyOnboardedException(String identitySubject) {
        super("A customer is already onboarded for identity " + identitySubject);
    }
}
