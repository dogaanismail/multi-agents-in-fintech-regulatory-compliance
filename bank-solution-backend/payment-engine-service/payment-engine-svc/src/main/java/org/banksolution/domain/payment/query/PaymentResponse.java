package org.banksolution.domain.payment.query;


import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String paymentId,
        String referenceNumber,
        String customerId,
        String sourceAccountId,
        String destinationAccountId,
        BigDecimal amount,
        String currency,
        PaymentType paymentType,
        String description,
        boolean isCrossBorderPayment,
        PaymentStatus status,
        FraudAnalysisStatus fraudStatus,
        RiskAssessment riskAssessment,
        Long version,

        // Lifecycle timestamps
        Instant initiatedAt,
        Instant riskAssessmentRequestedAt,
        Instant riskAssessmentCompletedAt,
        Instant fraudCheckApprovedAt,
        Instant manualReviewRequestedAt,
        Instant manualReviewApprovedAt,
        Instant manualReviewRejectedAt,
        Instant accountChargeInitiatedAt,
        Instant accountChargedAt,
        Instant accountChargeFailedAt,
        Instant completedAt,
        Instant blockedAt,

        // Decision metadata
        String manualReviewedBy,
        String manualReviewNotes,
        String blockReason,
        String failureReason,

        // Decision override metadata
        String decisionOverriddenBy,
        String decisionOverrideReason,
        Instant decisionOverriddenAt
) {
}
