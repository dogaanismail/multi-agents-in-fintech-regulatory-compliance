package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.FixedSide;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to initiate a payment")
public class PaymentRequest {

    @Schema(description = "Customer requesting the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @Schema(description = "Account to debit", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID sourceAccountId;

    @Schema(description = "Account to credit", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID destinationAccountId;

    @Schema(description = "Amount in the source currency", example = "100.00")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Schema(description = "Source currency", example = "EUR")
    @NotNull(message = "From currency is required")
    private Currency fromCurrency;

    @Schema(description = "Target currency", example = "GBP")
    @NotNull(message = "To currency is required")
    private Currency toCurrency;

    @Schema(description = "Type of payment", example = "TRANSFER_OUT")
    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @Schema(description = "Side whose amount is fixed during conversion", example = "SELL")
    @Builder.Default
    private FixedSide fixedSide = FixedSide.SELL;

    @Schema(description = "Free-text payment description")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

}


