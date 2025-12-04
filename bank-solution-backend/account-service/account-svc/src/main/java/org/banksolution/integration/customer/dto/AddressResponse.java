package org.banksolution.integration.customer.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private UUID id;
    private String city;
    private String countryCode;
}

