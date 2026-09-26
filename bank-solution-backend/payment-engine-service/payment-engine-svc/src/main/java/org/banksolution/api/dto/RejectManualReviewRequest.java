package org.banksolution.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Compliance officer rejection of a payment under review")
public class RejectManualReviewRequest {

    @Schema(description = "Payment under review", example = "3f2b8c1e-7a4d-4e9b-9c2a-5d1f6e8a0b47")
    private UUID paymentId;

    @Schema(description = "Compliance officer rejecting the payment")
    private String rejectedBy;

    @Schema(description = "Reason for blocking the payment")
    private String rejectionReason;

}
