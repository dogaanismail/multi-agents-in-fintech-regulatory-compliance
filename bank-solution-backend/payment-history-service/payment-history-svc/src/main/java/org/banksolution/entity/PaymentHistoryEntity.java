package org.banksolution.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_history")
public class PaymentHistoryEntity {

    @Id
    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "reference_number", unique = true, nullable = false)
    private String referenceNumber;

    // Payment Details
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "payment_type", length = 50)
    private String paymentType;

    @Column(name = "description", length = 1000)
    private String description;

    // Status Tracking
    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "fraud_status", length = 50)
    private String fraudStatus;

    // Risk Assessment from Risk-Engine
    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "risk_action", length = 20)
    private String riskAction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fraud_indicators", columnDefinition = "jsonb")
    private List<String> fraudIndicators;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "marl_assessment", columnDefinition = "jsonb")
    private MarlAssessment marlAssessment;

    @Column(name = "initiated_at")
    private Instant initiatedAt;

    @Column(name = "risk_check_requested_at")
    private Instant riskCheckRequestedAt;

    @Column(name = "risk_check_completed_at")
    private Instant riskCheckCompletedAt;

    @Column(name = "fraud_check_approved_at")
    private Instant fraudCheckApprovedAt;

    @Column(name = "manual_review_requested_at")
    private Instant manualReviewRequestedAt;

    @Column(name = "manual_review_approved_at")
    private Instant manualReviewApprovedAt;

    @Column(name = "manual_review_rejected_at")
    private Instant manualReviewRejectedAt;

    @Column(name = "manual_reviewed_by", length = 255)
    private String manualReviewedBy;

    @Column(name = "manual_review_notes", length = 2000)
    private String manualReviewNotes;

    @Column(name = "account_charge_initiated_at")
    private Instant accountChargeInitiatedAt;

    @Column(name = "account_charged_at")
    private Instant accountChargedAt;

    @Column(name = "account_charge_failed_at")
    private Instant accountChargeFailedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "block_reason", length = 1000)
    private String blockReason;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    // Processing Metadata
    @Column(name = "risk_processing_time_ms")
    private Long riskProcessingTimeMs;

    @Column(name = "marl_processing_time_ms")
    private Long marlProcessingTimeMs;

    @Column(name = "ml_model_version", length = 50)
    private String mlModelVersion;

    @Column(name = "aggregate_version")
    private Integer aggregateVersion;

    @Version
    @Column(name = "entity_version")
    private Short entityVersion;

    @Column(name = "created_at", nullable = false)
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarlAssessment {
        private String requestId;
        private String action;
        private Double confidence;
        private Double maddpgQValue;

        private AgentObservation transactionAgentObservation;
        private AgentObservation customerAgentObservation;
        private AgentObservation networkAgentObservation;

        private Map<String, Double> agentContributions;
        private Long processingTimeMs;
        private String mode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentObservation {
        private String agentName;
        private Boolean isSuspicious;
        private Double probability;
        private Double riskScore;
        private String confidence;
        private Double responseTimeMs;
    }

}
