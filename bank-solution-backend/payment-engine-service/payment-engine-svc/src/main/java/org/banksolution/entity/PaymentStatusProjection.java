package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_status_view")
public class PaymentStatusProjection {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "external_payment_id")
    private UUID externalPaymentId;

    @Column(name = "reference_number", unique = true)
    private String referenceNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_status", length = 50)
    private FraudCheckStatus fraudStatus;

    @Column(name = "fraud_confidence", precision = 5, scale = 2)
    private Double fraudConfidence;

    @Column(name = "maddpg_q_value", precision = 10, scale = 6)
    private Double maddpgQValue;

    @Column(name = "initiated_at")
    private LocalDateTime initiatedAt;

    @Column(name = "fraud_checked_at")
    private LocalDateTime fraudCheckedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
