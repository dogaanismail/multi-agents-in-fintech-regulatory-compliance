package org.banksolution.exception;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(UUID id) {
        super("Customer not found with id: " + id);
    }

    private CustomerNotFoundException(String message) {
        super(message);
    }

    public static CustomerNotFoundException forIdentitySubject(String identitySubject) {
        return new CustomerNotFoundException("No customer is onboarded for identity " + identitySubject);
    }
}
