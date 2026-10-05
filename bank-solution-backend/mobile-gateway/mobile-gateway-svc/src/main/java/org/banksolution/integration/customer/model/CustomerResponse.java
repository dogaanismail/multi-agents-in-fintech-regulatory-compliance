package org.banksolution.integration.customer.model;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String email) {
}
