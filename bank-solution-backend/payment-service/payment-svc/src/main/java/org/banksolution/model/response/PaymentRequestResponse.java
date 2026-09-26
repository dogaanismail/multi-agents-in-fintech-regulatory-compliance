package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Accepted payment request with conversion details")
public class PaymentRequestResponse {

    @Schema(description = "Payment identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Customer who requested the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID customerId;

    @Schema(description = "Account debited by the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID sourceAccountId;

    @Schema(description = "Account credited by the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID destinationAccountId;

    @Schema(description = "Amount in the source currency", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Source currency", example = "EUR")
    private Currency fromCurrency;

    @Schema(description = "Type of payment", example = "TRANSFER_OUT")
    private PaymentType paymentType;

    @Schema(description = "Free-text payment description")
    private String description;

    @Schema(description = "Amount in the target currency", example = "85.20")
    private BigDecimal convertedAmount;

    @Schema(description = "Target currency", example = "GBP")
    private Currency toCurrency;

    @Schema(description = "Exchange rate applied to the conversion", example = "0.8520")
    private BigDecimal appliedExchangeRate;

    @Schema(description = "When the payment was requested", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "Outcome message for the request")
    private String message;

}

