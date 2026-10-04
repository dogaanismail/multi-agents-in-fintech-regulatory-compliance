package org.banksolution.outbox.relay;

public class OutboxDeliveryException extends RuntimeException {

    public OutboxDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
