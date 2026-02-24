package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import com.aml.feedback.FeedbackType;
import com.aml.feedback.OfficerDecision;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class ComplianceAgentManualFeedbackEventMapper {

    public static ComplianceAgentManualFeedbackEvent toAvroEvent(
            String paymentId,
            String feedbackType,
            String originalMarlAction,
            String officerDecision,
            String reviewedBy,
            String notes
    ) {
        return ComplianceAgentManualFeedbackEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(paymentId)
                .setFeedbackType(FeedbackType.valueOf(feedbackType))
                .setOriginalMarlAction(originalMarlAction)
                .setOfficerDecision(OfficerDecision.valueOf(officerDecision))
                .setReviewedBy(reviewedBy)
                .setNotes(notes)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }
}
