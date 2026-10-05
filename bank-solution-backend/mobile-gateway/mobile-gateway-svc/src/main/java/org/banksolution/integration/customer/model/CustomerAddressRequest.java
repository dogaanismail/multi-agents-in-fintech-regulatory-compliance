package org.banksolution.integration.customer.model;

public record CustomerAddressRequest(
        String city,
        String countryCode) {
}
