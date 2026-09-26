package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Amount moved against one customer account")
public class CustomerAccountMovementRequest {

    @Schema(description = "Amount in major units", example = "250.00")
    @NotNull(message = "Amount can't be null.")
    @Positive(message = "Amount must be positive.")
    private BigDecimal amount;

    @Schema(description = "Movement currency (ISO 4217)", example = "GBP")
    @NotNull(message = "Currency can't be null.")
    private Currency currency;

    @Schema(description = "Customer bank account being moved", example = "7c2e9f4a-3b1d-4c8e-9a6f-5d0b2e1c7a83")
    @NotNull(message = "Customer account ID can't be null.")
    private UUID customerAccountId;

    @Schema(description = "Facing internal account; defaults to clearing", example = "INBOUND_CLEARING")
    private LedgerAccountType internalAccountType;

}
