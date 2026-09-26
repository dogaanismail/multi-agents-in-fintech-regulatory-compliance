package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.banksolution.entity.enums.CustomerStatus;
import org.banksolution.entity.enums.CustomerType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Customer profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    @Schema(description = "Customer id", example = "3f2a7c1e-8b4d-4e6a-9c2f-1d5e7b9a0c34")
    private UUID id;

    @Schema(description = "Customer's first name")
    private String firstName;

    @Schema(description = "Customer's last name")
    private String lastName;

    @Schema(description = "Customer's middle name")
    private String middleName;

    @Schema(description = "Customer's email address", example = "alice@example.com")
    private String email;

    @Schema(description = "Phone number in E.164 format", example = "+447911123456")
    private String phoneNumber;

    @Schema(description = "Customer's date of birth", example = "1990-04-15")
    private LocalDate dateOfBirth;

    @Schema(description = "ISO 3166-1 alpha-2 nationality code", example = "GB")
    private String nationality;

    @Schema(description = "Kind of customer", example = "INDIVIDUAL")
    private CustomerType customerType;

    @Schema(description = "Customer's lifecycle status", example = "ACTIVE")
    private CustomerStatus customerStatus;

    @Schema(description = "Customer's address")
    private AddressResponse address;

    @Schema(description = "When the customer was created", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "When the customer was last updated", example = "2026-01-15T10:30:00Z")
    private Instant updatedAt;

}

