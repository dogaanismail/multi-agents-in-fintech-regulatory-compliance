package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.banksolution.enums.PaymentType;
import org.banksolution.enums.RiskCheckStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.banksolution.enums.RiskCheckStatus.AWAITING_MARL;

@Entity
@Table(name = "risk_check_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskCheckRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "source_account_id")
    private String sourceAccountId;

    @Column(name = "destination_account_id")
    private String destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(name = "description")
    private String description;

    @Column(name = "request_timestamp", nullable = false)
    private Long requestTimestamp;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RiskCheckStatus status = AWAITING_MARL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
