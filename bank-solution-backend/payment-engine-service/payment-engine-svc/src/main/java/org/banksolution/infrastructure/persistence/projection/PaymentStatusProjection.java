package org.banksolution.infrastructure.persistence.projection;

import jakarta.persistence.*;
import lombok.*;
import org.banksolution.enums.FraudCheckStatus;
import org.banksolution.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "risk_action", length = 20)
    private String riskAction;

    @Column(name = "initiated_at")
    private Instant initiatedAt;

    @Column(name = "risk_check_completed_at")
    private Instant riskCheckCompletedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
