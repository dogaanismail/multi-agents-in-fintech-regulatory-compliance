package org.banksolution.model.response;

import java.util.UUID;

public record MobileProfileResponse(
        UUID customerId,
        String firstName,
        String lastName,
        String email) {
}
