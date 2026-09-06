package org.banksolution.exception;

public class KafkaPublicationException extends RuntimeException {

    public KafkaPublicationException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
