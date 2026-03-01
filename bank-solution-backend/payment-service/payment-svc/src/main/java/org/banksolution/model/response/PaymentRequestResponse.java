package org.banksolution.model.response;

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
public class PaymentRequestResponse {
    private UUID id;
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private Currency fromCurrency;
    private PaymentType paymentType;
    private String description;
    private BigDecimal convertedAmount;
    private Currency toCurrency;
    private BigDecimal appliedExchangeRate;
    private Instant createdAt;
    private String message;
}

