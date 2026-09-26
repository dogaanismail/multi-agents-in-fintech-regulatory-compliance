package org.banksolution.exception;

import java.util.UUID;

public class CustomerServiceUnavailableException extends RuntimeException {

    public CustomerServiceUnavailableException(UUID customerId, Throwable cause) {
        super("Could not verify customer " + customerId + ": customer-service is unavailable", cause);
    }
}
