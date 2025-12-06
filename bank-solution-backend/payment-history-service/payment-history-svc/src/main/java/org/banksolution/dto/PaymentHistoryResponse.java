package org.banksolution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for payment history API responses.
 * Used for pagination and API layer data transfer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistoryResponse {

    private UUID paymentId;
    private UUID externalPaymentId;
    private String referenceNumber;

    // Payment Details
    private UUID customerId;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String paymentType;
    private String description;

    // Status Tracking
    private String status;
    private String fraudStatus;

    // Risk Assessment
    private Double riskScore;
    private String riskLevel;
    private String riskAction;
    private List<String> fraudIndicators;

    // MARL Assessment
    private MarlAssessmentDto marlAssessment;

    // Lifecycle Timestamps
    private Instant initiatedAt;
    private Instant riskCheckRequestedAt;
    private Instant riskCheckCompletedAt;
    private Instant completedAt;
    private Instant blockedAt;

    private Long riskProcessingTimeMs;
    private Long marlProcessingTimeMs;
    private String mlModelVersion;
    private Integer aggregateVersion;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarlAssessmentDto {
        private String requestId;
        private String action;
        private Double confidence;
        private Double maddpgQValue;
        private AgentObservationDto transactionAgentObservation;
        private AgentObservationDto customerAgentObservation;
        private AgentObservationDto networkAgentObservation;
        private Map<String, Double> agentContributions;
        private Long processingTimeMs;
        private String mode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentObservationDto {
        private String agentName;
        private Boolean isSuspicious;
        private Double probability;
        private Double riskScore;
        private String confidence;
        private Double responseTimeMs;
    }
}
