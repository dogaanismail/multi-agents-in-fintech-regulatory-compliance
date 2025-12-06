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
    private String referenceNumber;
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private Currency currency;
    private PaymentType paymentType;
    private String description;
    private Instant createdAt;
    private String message;
}

