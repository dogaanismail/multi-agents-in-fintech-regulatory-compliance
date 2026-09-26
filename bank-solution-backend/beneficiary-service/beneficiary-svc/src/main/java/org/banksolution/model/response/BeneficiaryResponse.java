package org.banksolution.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.banksolution.enums.BeneficiaryStatus;
import org.banksolution.enums.BeneficiaryType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Beneficiary with its active payout coordinates")
public class BeneficiaryResponse {

    @Schema(description = "Beneficiary identifier", example = "3f1c9a7e-5b2d-4e8f-a6c1-9d0e7b3a2f58")
    private UUID id;

    @Schema(description = "Customer who owns the beneficiary", example = "8a365c7c-066c-498b-92b3-1582a4c001c1")
    private UUID customerId;

    @Schema(description = "Individual or company beneficiary", example = "INDIVIDUAL")
    private BeneficiaryType type;

    @Schema(description = "Alias or nickname")
    private String alias;

    @Schema(description = "Company name, for company beneficiaries")
    private String companyName;

    @Schema(description = "First name, for individual beneficiaries")
    private String firstName;

    @Schema(description = "Last name, for individual beneficiaries")
    private String lastName;

    @Schema(description = "Lifecycle status", example = "ACTIVE")
    private BeneficiaryStatus beneficiaryStatus;

    @Schema(description = "Payout coordinates of the beneficiary")
    private List<BeneficiaryCoordinateResponse> coordinates;

    @Schema(description = "When the beneficiary was created", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "When the beneficiary was last updated", example = "2026-09-27T10:15:30Z")
    private Instant updatedAt;

}
