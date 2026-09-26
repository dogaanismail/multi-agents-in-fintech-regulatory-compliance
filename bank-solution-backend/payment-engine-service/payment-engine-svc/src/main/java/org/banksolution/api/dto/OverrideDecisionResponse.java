package org.banksolution.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Outcome of a decision override")
public class OverrideDecisionResponse {

    @Schema(description = "Payment whose decision was overridden", example = "3f2b8c1e-7a4d-4e9b-9c2a-5d1f6e8a0b47")
    private String paymentId;

    @Schema(description = "Human-readable outcome message")
    private String message;

    @Schema(description = "Compliance officer who applied the override")
    private String overriddenBy;

    @Schema(description = "Payment status after the override", example = "OVERRIDE_APPROVED")
    private String newStatus;
}
