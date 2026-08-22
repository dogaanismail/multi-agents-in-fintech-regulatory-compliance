package org.banksolution.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * How much to move, for which customer account, and which internal account faces it.
 * Shared by the authorisation and hard-settlement instructions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAccountMovementRequest {

    @NotNull(message = "Amount can't be null.")
    @Positive(message = "Amount must be positive.")
    private BigDecimal amount;

    @NotNull(message = "Currency can't be null.")
    private Currency currency;

    @NotNull(message = "Customer account ID can't be null.")
    private UUID customerAccountId;

    private LedgerAccountType internalAccountType;

}
