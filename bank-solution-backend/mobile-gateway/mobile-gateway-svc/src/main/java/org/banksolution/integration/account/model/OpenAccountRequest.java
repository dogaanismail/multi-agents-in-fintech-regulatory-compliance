package org.banksolution.integration.account.model;

import java.util.List;
import java.util.UUID;

public record OpenAccountRequest(
        UUID customerId,
        String accountType,
        String bankLocation,
        List<String> currencies) {
}
