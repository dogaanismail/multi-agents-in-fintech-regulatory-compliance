package org.banksolution.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Acknowledgement of an initiated payment")
public class InitiatePaymentResponse {

    @Schema(description = "Id of the initiated payment", example = "3f2b8c1e-7a4d-4e9b-9c2a-5d1f6e8a0b47")
    private String paymentId;

    @Schema(description = "Human-readable outcome message")
    private String message;
}
