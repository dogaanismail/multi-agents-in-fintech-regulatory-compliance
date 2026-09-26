package org.banksolution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Full lifecycle history of a payment")
public class PaymentHistoryResponse {

    @Schema(description = "Payment identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID paymentId;

    @Schema(description = "Human-readable payment reference")
    private String referenceNumber;

    // Payment Details
    @Schema(description = "Customer who made the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID customerId;

    @Schema(description = "Account debited by the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID sourceAccountId;

    @Schema(description = "Account credited by the payment", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID destinationAccountId;

    @Schema(description = "Amount in the source currency", example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Source currency", example = "EUR")
    private String fromCurrency;

    @Schema(description = "Target currency", example = "GBP")
    private String toCurrency;

    @Schema(description = "Amount in the target currency", example = "85.20")
    private BigDecimal convertedAmount;

    @Schema(description = "Exchange rate applied to the conversion", example = "0.8520")
    private BigDecimal appliedExchangeRate;

    @Schema(description = "Type of payment", example = "TRANSFER_OUT")
    private String paymentType;

    @Schema(description = "Free-text payment description")
    private String description;

    // Status Tracking
    @Schema(description = "Current payment status", example = "COMPLETED")
    private String status;

    @Schema(description = "Fraud check status", example = "APPROVED")
    private String fraudStatus;

    // Risk Assessment
    @Schema(description = "Risk score from the risk engine", example = "0.12")
    private Double riskScore;

    @Schema(description = "Assessed risk level", example = "LOW")
    private String riskLevel;

    @Schema(description = "Action recommended by the risk engine", example = "PROCEED")
    private String riskAction;

    @Schema(description = "Fraud indicators raised during assessment")
    private List<String> fraudIndicators;

    // MARL Assessment
    @Schema(description = "Multi-agent fraud assessment details")
    private MarlAssessmentDto marlAssessment;

    // Complete Lifecycle Timestamps - Full audit trail
    @Schema(description = "When the payment was initiated", example = "2026-09-27T10:15:30Z")
    private Instant initiatedAt;

    @Schema(description = "When risk assessment was requested", example = "2026-09-27T10:15:30Z")
    private Instant riskCheckRequestedAt;

    @Schema(description = "When risk assessment completed", example = "2026-09-27T10:15:30Z")
    private Instant riskCheckCompletedAt;

    @Schema(description = "When the fraud check approved the payment", example = "2026-09-27T10:15:30Z")
    private Instant fraudCheckApprovedAt;

    @Schema(description = "When manual review was requested", example = "2026-09-27T10:15:30Z")
    private Instant manualReviewRequestedAt;

    @Schema(description = "When manual review approved the payment", example = "2026-09-27T10:15:30Z")
    private Instant manualReviewApprovedAt;

    @Schema(description = "When manual review rejected the payment", example = "2026-09-27T10:15:30Z")
    private Instant manualReviewRejectedAt;

    @Schema(description = "When ledger authorisation started", example = "2026-09-27T10:15:30Z")
    private Instant ledgerAuthorisationInitiatedAt;

    @Schema(description = "When the ledger authorised the funds", example = "2026-09-27T10:15:30Z")
    private Instant ledgerAuthorisedAt;

    @Schema(description = "When ledger settlement started", example = "2026-09-27T10:15:30Z")
    private Instant ledgerSettlementInitiatedAt;

    @Schema(description = "When the ledger settled the payment", example = "2026-09-27T10:15:30Z")
    private Instant ledgerSettledAt;

    @Schema(description = "When release of held funds started", example = "2026-09-27T10:15:30Z")
    private Instant ledgerReleaseInitiatedAt;

    @Schema(description = "When held funds were released", example = "2026-09-27T10:15:30Z")
    private Instant ledgerReleasedAt;

    @Schema(description = "When the payment completed", example = "2026-09-27T10:15:30Z")
    private Instant completedAt;

    @Schema(description = "When the payment was blocked", example = "2026-09-27T10:15:30Z")
    private Instant blockedAt;

    // Decision Metadata
    @Schema(description = "Compliance officer who reviewed the payment")
    private String manualReviewedBy;

    @Schema(description = "Notes from the manual review")
    private String manualReviewNotes;

    @Schema(description = "Reason the payment was blocked")
    private String blockReason;

    @Schema(description = "Reason the payment failed")
    private String failureReason;

    // Decision Override Metadata
    @Schema(description = "Officer who overrode the decision")
    private String decisionOverriddenBy;

    @Schema(description = "Reason for the decision override")
    private String decisionOverrideReason;

    @Schema(description = "When the decision was overridden", example = "2026-09-27T10:15:30Z")
    private Instant decisionOverriddenAt;

    // Processing Metadata
    @Schema(description = "Risk assessment duration in milliseconds")
    private Long riskProcessingTimeMs;

    @Schema(description = "Multi-agent assessment duration in milliseconds")
    private Long marlProcessingTimeMs;

    @Schema(description = "Version of the fraud model used")
    private String mlModelVersion;

    @Schema(description = "Version of the source payment aggregate")
    private Integer aggregateVersion;

    @Schema(description = "When the history record was created", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

    @Schema(description = "When the history record was last updated", example = "2026-09-27T10:15:30Z")
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Multi-agent RL fraud assessment")
    public static class MarlAssessmentDto {

        @Schema(description = "Fraud analysis request identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        private String requestId;

        @Schema(description = "Action chosen by the orchestrator", example = "ALLOW")
        private String action;

        @Schema(description = "Decision confidence between 0 and 1")
        private Double confidence;

        @Schema(description = "Critic Q-value for the chosen action")
        private Double maddpgQValue;

        @Schema(description = "Transaction pattern agent observation")
        private AgentObservationDto transactionAgentObservation;

        @Schema(description = "Customer risk agent observation")
        private AgentObservationDto customerAgentObservation;

        @Schema(description = "Network analysis agent observation")
        private AgentObservationDto networkAgentObservation;

        @Schema(description = "Weight of each agent in the decision")
        private Map<String, Double> agentContributions;

        @Schema(description = "Assessment duration in milliseconds")
        private Long processingTimeMs;

        @Schema(description = "Orchestrator execution mode", example = "inference")
        private String mode;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Single agent fraud observation")
    public static class AgentObservationDto {

        @Schema(description = "Name of the reporting agent")
        private String agentName;

        @Schema(description = "Whether the agent flagged the payment")
        private Boolean isSuspicious;

        @Schema(description = "Predicted fraud probability")
        private Double probability;

        @Schema(description = "Risk score from this agent")
        private Double riskScore;

        @Schema(description = "Agent confidence band")
        private String confidence;

        @Schema(description = "Agent response time in milliseconds")
        private Double responseTimeMs;

        @Schema(description = "Per-feature SHAP contributions")
        private java.util.List<FeatureContributionDto> featureContributions;

        @Schema(description = "SHAP base value of the model")
        private Double shapBaseValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "SHAP contribution of one feature")
    public static class FeatureContributionDto {

        @Schema(description = "Feature name")
        private String feature;

        @Schema(description = "Feature value for this payment")
        private String value;

        @Schema(description = "SHAP contribution to the prediction")
        private Double shapValue;

        @Schema(description = "Effect on risk", example = "INCREASES_RISK")
        private String direction;

    }
}
