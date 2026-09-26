package org.banksolution.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance officer override of an automated decision")
public class OverrideDecisionRequest {

    @Schema(description = "Payment whose decision is overridden", example = "3f2b8c1e-7a4d-4e9b-9c2a-5d1f6e8a0b47")
    private UUID paymentId;

    @Schema(description = "Compliance officer applying the override")
    private String overriddenBy;

    @Schema(description = "Reason for overriding the decision")
    private String overrideReason;

    @Schema(description = "True to approve, false to reject")
    private boolean approvePayment;
}
