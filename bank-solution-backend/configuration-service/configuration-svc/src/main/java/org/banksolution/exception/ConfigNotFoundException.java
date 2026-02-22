package org.banksolution.exception;

import java.util.UUID;

public class ConfigNotFoundException extends RuntimeException {

    public ConfigNotFoundException(UUID id) {
        super("Configuration not found with id: " + id);
    }

    public ConfigNotFoundException(String key) {
        super("Configuration not found with key: " + key);
    }
}
