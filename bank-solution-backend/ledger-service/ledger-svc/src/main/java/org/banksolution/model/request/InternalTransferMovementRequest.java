package org.banksolution.model.request;

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
public class InternalTransferMovementRequest {

    @NotNull(message = "Amount can't be null.")
    @Positive(message = "Amount must be positive.")
    private BigDecimal amount;

    @NotNull(message = "Currency can't be null.")
    private Currency currency;

    @NotNull(message = "Source customer account ID can't be null.")
    private UUID sourceCustomerAccountId;

    @NotNull(message = "Destination customer account ID can't be null.")
    private UUID destinationCustomerAccountId;

}
