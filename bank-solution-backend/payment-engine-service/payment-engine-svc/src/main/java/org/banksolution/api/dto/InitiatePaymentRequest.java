package org.banksolution.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start a payment through the compliance chain")
public class InitiatePaymentRequest {

    @Schema(description = "Client-supplied payment id; generated if absent", example = "3f2b8c1e-7a4d-4e9b-9c2a-5d1f6e8a0b47")
    private UUID paymentId;

    @Schema(description = "Customer initiating the payment", example = "8c6d2a91-1f3e-4b7a-a5c4-2e9d7f0b1c36")
    private UUID customerId;

    @Schema(description = "Account to debit", example = "b1e4f7a2-5c8d-4a3b-9e6f-0d2c7a9b4e15")
    private UUID sourceAccountId;

    @Schema(description = "Account to credit", example = "e7a9c3d5-2b6f-4d1e-8a4c-9f0b3e5d7a28")
    private UUID destinationAccountId;

    @Schema(description = "Payment amount in major units", example = "250.00")
    private BigDecimal amount;

    @Schema(description = "Source currency, ISO 4217", example = "GBP")
    private String fromCurrency;

    @Schema(description = "Target currency; defaults to source currency", example = "GBP")
    private String toCurrency;

    @Schema(description = "Kind of payment", example = "TRANSFER_OUT")
    private String paymentType;

    @Schema(description = "Whether the payment crosses borders")
    private boolean crossBorderPayment;

    @Schema(description = "Free-text payment reference")
    private String description;
}
