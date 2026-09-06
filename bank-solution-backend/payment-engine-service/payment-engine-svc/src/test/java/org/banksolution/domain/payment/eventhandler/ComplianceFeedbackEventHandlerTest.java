package org.banksolution.domain.payment.eventhandler;

import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.exception.PaymentNotFoundException;
import org.banksolution.infrastructure.messaging.kafka.producer.ComplianceAgentManualFeedbackEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceFeedbackEventHandlerTest {

    private static final EventMessage<?> EVENT_MESSAGE = GenericEventMessage.asEventMessage("ignored");

    @Mock
    private ComplianceAgentManualFeedbackEventProducer complianceAgentManualFeedbackEventProducer;

    @Mock
    private PaymentQueryService paymentQueryService;

    @InjectMocks
    private ComplianceFeedbackEventHandler complianceFeedbackEventHandler;

    @Test
    void shouldReportAnApprovalAgainstTheActionTheAgentsRecommended() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(createPaymentResponse(
                PaymentStatus.SETTLEMENT_PENDING, FraudAnalysisStatus.APPROVED,
                createRiskAssessmentWithMarl("ESCALATE", "MEDIUM", 0.6)));

        complianceFeedbackEventHandler.on(createManualReviewApprovedEvent(), EVENT_MESSAGE);

        verify(complianceAgentManualFeedbackEventProducer).publish(
                PAYMENT_UUID.toString(),
                "MANUAL_REVIEW",
                "BLOCK",
                "APPROVE",
                OFFICER,
                APPROVAL_NOTES);
    }

    @Test
    void shouldReportARejectionWithAnUnknownActionWhenNoMarlAssessmentExists() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(createPaymentResponse(
                PaymentStatus.RELEASE_PENDING, FraudAnalysisStatus.BLOCKED, createEscalateAssessment()));

        complianceFeedbackEventHandler.on(createManualReviewRejectedEvent(), EVENT_MESSAGE);

        verify(complianceAgentManualFeedbackEventProducer).publish(
                PAYMENT_UUID.toString(), "MANUAL_REVIEW", "UNKNOWN", "REJECT", OFFICER, REJECTION_REASON);
    }

    @Test
    void shouldReportAnUnknownActionWhenThePaymentCarriesNoRiskAssessment() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(createPaymentResponse(
                PaymentStatus.RELEASE_PENDING, FraudAnalysisStatus.BLOCKED, null));

        complianceFeedbackEventHandler.on(createManualReviewRejectedEvent(), EVENT_MESSAGE);

        verify(complianceAgentManualFeedbackEventProducer).publish(
                PAYMENT_UUID.toString(), "MANUAL_REVIEW", "UNKNOWN", "REJECT", OFFICER, REJECTION_REASON);
    }

    @Test
    void shouldFailTheHandlerWhenThePaymentCannotBeLoadedSoTheEventIsDeadLettered() {
        PaymentNotFoundException paymentNotFoundException = new PaymentNotFoundException("missing %s", null, PAYMENT_UUID);
        when(paymentQueryService.findPaymentById(createPaymentId())).thenThrow(paymentNotFoundException);

        assertThatThrownBy(() -> complianceFeedbackEventHandler.on(createManualReviewApprovedEvent(), EVENT_MESSAGE))
                .isSameAs(paymentNotFoundException);

        verifyNoInteractions(complianceAgentManualFeedbackEventProducer);
    }

    @Test
    void shouldTranslateAnOverrideIntoFeedbackAgainstTheOriginalDecision() {
        complianceFeedbackEventHandler.on(createDecisionOverriddenEvent(true, "BLOCKED"), EVENT_MESSAGE);
        complianceFeedbackEventHandler.on(createDecisionOverriddenEvent(false, "COMPLETED"), EVENT_MESSAGE);
        complianceFeedbackEventHandler.on(createDecisionOverriddenEvent(false, "FAILED"), EVENT_MESSAGE);

        verify(complianceAgentManualFeedbackEventProducer).publish(
                PAYMENT_UUID.toString(), "DECISION_OVERRIDE", "BLOCK", "APPROVE", OFFICER, OVERRIDE_REASON);
        verify(complianceAgentManualFeedbackEventProducer).publish(
                PAYMENT_UUID.toString(), "DECISION_OVERRIDE", "ALLOW", "REJECT", OFFICER, OVERRIDE_REASON);
        verify(complianceAgentManualFeedbackEventProducer).publish(
                PAYMENT_UUID.toString(), "DECISION_OVERRIDE", "UNKNOWN", "REJECT", OFFICER, OVERRIDE_REASON);
    }

    @Test
    void shouldLetAFailedOverridePublicationFailTheHandlerSoItIsRetriedOrDeadLettered() {
        IllegalStateException brokerFailure = new IllegalStateException("kafka down");
        doThrow(brokerFailure)
                .when(complianceAgentManualFeedbackEventProducer)
                .publish(PAYMENT_UUID.toString(), "DECISION_OVERRIDE", "BLOCK", "APPROVE", OFFICER, OVERRIDE_REASON);

        assertThatThrownBy(() -> complianceFeedbackEventHandler.on(
                createDecisionOverriddenEvent(true, "BLOCKED"), EVENT_MESSAGE))
                .isSameAs(brokerFailure);
    }

    @Test
    void shouldLetAFailedManualReviewPublicationFailTheHandlerSoItIsRetriedOrDeadLettered() {
        when(paymentQueryService.findPaymentById(createPaymentId())).thenReturn(
                createPaymentResponse(PaymentStatus.RELEASE_PENDING, FraudAnalysisStatus.BLOCKED, createEscalateAssessment()));
        IllegalStateException brokerFailure = new IllegalStateException("kafka down");
        doThrow(brokerFailure)
                .when(complianceAgentManualFeedbackEventProducer)
                .publish(PAYMENT_UUID.toString(), "MANUAL_REVIEW", "UNKNOWN", "REJECT", OFFICER, REJECTION_REASON);

        assertThatThrownBy(() -> complianceFeedbackEventHandler.on(createManualReviewRejectedEvent(), EVENT_MESSAGE))
                .isSameAs(brokerFailure);
    }
}
