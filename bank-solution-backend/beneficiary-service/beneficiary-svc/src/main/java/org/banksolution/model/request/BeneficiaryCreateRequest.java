package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.banksolution.enums.BeneficiaryType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to create a new beneficiary for a customer")
public class BeneficiaryCreateRequest {

    @NotNull(message = "The customer ID cannot be null")
    @Schema(
            description = "The unique identifier of the customer to whom the beneficiary will be associated",
            example = "8a365c7c-066c-498b-92b3-1582a4c001c1"
    )
    private UUID customerId;

    @Schema(description = "Alias or nickname for the beneficiary", example = "Johnny")
    private String alias;

    @NotNull(message = "Beneficiary type cannot be null")
    @Schema(description = "The type of beneficiary: INDIVIDUAL or COMPANY", example = "INDIVIDUAL")
    private BeneficiaryType type;

    @Schema(description = "First name of the beneficiary (required if type = INDIVIDUAL)", example = "John")
    private String firstName;

    @Schema(description = "Last name of the beneficiary (required if type = INDIVIDUAL)", example = "Doe")
    private String lastName;

    @Schema(description = "Company name (required if type = COMPANY)")
    private String companyName;

    @Valid
    @NotNull(message = "At least one set of beneficiary coordinates is required")
    @Size(min = 1, message = "At least one coordinate must be provided")
    @Schema(description = "List of beneficiary payout coordinates")
    private List<BeneficiaryCoordinateRequest> coordinates;
}
