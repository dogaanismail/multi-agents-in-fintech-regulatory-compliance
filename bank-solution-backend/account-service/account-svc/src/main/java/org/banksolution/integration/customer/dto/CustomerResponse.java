package org.banksolution.integration.customer.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String nationality;
    private CustomerType customerType;
    private CustomerStatus customerStatus;
    private AddressResponse address;
    private Instant createdAt;
    private Instant updatedAt;

}
