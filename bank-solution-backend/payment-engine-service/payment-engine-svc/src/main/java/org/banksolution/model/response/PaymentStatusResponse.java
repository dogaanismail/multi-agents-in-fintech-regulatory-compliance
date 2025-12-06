package org.banksolution.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {

    private UUID paymentId;
    private UUID externalPaymentId;
    private String referenceNumber;
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private String description;
    private PaymentStatus status;
    private FraudCheckStatus fraudStatus;
    private Double fraudConfidence;
    private Double maddpgQValue;
    private LocalDateTime initiatedAt;
    private LocalDateTime fraudCheckedAt;
    private LocalDateTime completedAt;
    private LocalDateTime blockedAt;
}
