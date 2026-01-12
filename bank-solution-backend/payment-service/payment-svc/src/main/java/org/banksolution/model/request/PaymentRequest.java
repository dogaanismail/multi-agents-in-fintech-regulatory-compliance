package org.banksolution.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    private UUID sourceAccountId;

    private UUID destinationAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    // TODO: Implement multi-currency support, we will keep it simple for conversion

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

}


