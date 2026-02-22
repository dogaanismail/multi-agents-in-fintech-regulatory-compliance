package org.banksolution.exception;

public class ConfigAlreadyExistsException extends RuntimeException {

    public ConfigAlreadyExistsException(String configKey) {
        super("Configuration already exists with key: " + configKey);
    }
}
