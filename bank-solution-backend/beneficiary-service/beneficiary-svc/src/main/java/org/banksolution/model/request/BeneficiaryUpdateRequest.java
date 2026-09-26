package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.banksolution.enums.BeneficiaryStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to update a beneficiary's details and status")
public class BeneficiaryUpdateRequest {

    @Schema(description = "Alias or nickname for the beneficiary", example = "Johnny")
    private String alias;

    @Schema(description = "First name of the beneficiary (INDIVIDUAL only)", example = "John")
    private String firstName;

    @Schema(description = "Last name of the beneficiary (INDIVIDUAL only)", example = "Doe")
    private String lastName;

    @Schema(description = "Company name (COMPANY only)")
    private String companyName;

    @NotNull(message = "Beneficiary status cannot be null")
    @Schema(description = "ACTIVE or INACTIVE; use DELETE to delete a beneficiary", example = "ACTIVE")
    private BeneficiaryStatus beneficiaryStatus;
}
