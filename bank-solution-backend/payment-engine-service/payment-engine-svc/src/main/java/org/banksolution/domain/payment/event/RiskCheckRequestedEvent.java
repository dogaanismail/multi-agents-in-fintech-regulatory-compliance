package org.banksolution.domain.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.banksolution.domain.payment.valueobject.PaymentId;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckRequestedEvent {
    private PaymentId paymentId;
    private String referenceNumber;
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private String description;
}
