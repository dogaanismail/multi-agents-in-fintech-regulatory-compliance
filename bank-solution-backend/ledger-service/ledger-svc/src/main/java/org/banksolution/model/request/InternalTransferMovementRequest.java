package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.banksolution.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Transfer between two customer accounts")
public class InternalTransferMovementRequest {

    @Schema(description = "Amount in major units", example = "250.00")
    @NotNull(message = "Amount can't be null.")
    @Positive(message = "Amount must be positive.")
    private BigDecimal amount;

    @Schema(description = "Transfer currency (ISO 4217)", example = "GBP")
    @NotNull(message = "Currency can't be null.")
    private Currency currency;

    @Schema(description = "Customer account being debited", example = "7c2e9f4a-3b1d-4c8e-9a6f-5d0b2e1c7a83")
    @NotNull(message = "Source customer account ID can't be null.")
    private UUID sourceCustomerAccountId;

    @Schema(description = "Customer account being credited", example = "e9a3c6b1-2d7f-4e8a-b5c4-8f1d0a3e6b27")
    @NotNull(message = "Destination customer account ID can't be null.")
    private UUID destinationCustomerAccountId;

}
