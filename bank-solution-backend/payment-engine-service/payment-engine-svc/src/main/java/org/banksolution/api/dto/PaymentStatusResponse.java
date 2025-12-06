package org.banksolution.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
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
    
    // Risk Assessment summary
    private Double riskScore;
    private String riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL
    private String riskAction; // PROCEED, ESCALATE, BLOCK
    
    // Timestamps
    private Instant initiatedAt;
    private Instant riskCheckCompletedAt;
    private Instant completedAt;
    private Instant blockedAt;
}
