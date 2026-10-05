package org.banksolution.integration.customer.model;

import java.time.LocalDate;

public record CustomerCreateRequest(
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        LocalDate dateOfBirth,
        String nationality,
        String customerType,
        CustomerAddressRequest address) {
}
