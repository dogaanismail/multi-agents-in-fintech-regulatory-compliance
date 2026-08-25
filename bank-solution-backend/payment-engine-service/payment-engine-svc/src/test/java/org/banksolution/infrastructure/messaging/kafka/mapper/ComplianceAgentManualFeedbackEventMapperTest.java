package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import com.aml.feedback.FeedbackType;
import com.aml.feedback.OfficerDecision;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;

class ComplianceAgentManualFeedbackEventMapperTest {

    @Test
    void shouldBuildTheFeedbackEventFromTheOfficerDecision() {
        ComplianceAgentManualFeedbackEvent complianceAgentManualFeedbackEvent = ComplianceAgentManualFeedbackEventMapper
                .toAvroEvent(PAYMENT_UUID.toString(), "MANUAL_REVIEW", "BLOCK", "APPROVE", OFFICER, APPROVAL_NOTES);

        assertThat(complianceAgentManualFeedbackEvent.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(complianceAgentManualFeedbackEvent.getFeedbackType()).isEqualTo(FeedbackType.MANUAL_REVIEW);
        assertThat(complianceAgentManualFeedbackEvent.getOriginalMarlAction()).isEqualTo("BLOCK");
        assertThat(complianceAgentManualFeedbackEvent.getOfficerDecision()).isEqualTo(OfficerDecision.APPROVE);
        assertThat(complianceAgentManualFeedbackEvent.getReviewedBy()).isEqualTo(OFFICER);
        assertThat(complianceAgentManualFeedbackEvent.getNotes()).isEqualTo(APPROVAL_NOTES);
        assertThat(complianceAgentManualFeedbackEvent.getEventId()).isNotBlank();
        assertThat(complianceAgentManualFeedbackEvent.getTimestamp()).isPositive();
    }

    @Test
    void shouldRejectAFeedbackTypeOutsideTheContract() {
        assertThatThrownBy(() -> ComplianceAgentManualFeedbackEventMapper
                .toAvroEvent(PAYMENT_UUID.toString(), "GUT_FEELING", "BLOCK", "APPROVE", OFFICER, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
