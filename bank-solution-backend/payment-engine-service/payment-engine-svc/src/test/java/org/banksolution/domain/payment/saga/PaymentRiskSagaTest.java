package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskAssessmentRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentRiskSagaTest {

    private static final Duration PAST_THE_RISK_TIMEOUT = Duration.ofMinutes(2);

    private SagaTestFixture<PaymentRiskSaga> fixture;
    private RiskAssessmentRequestedEventProducer riskAssessmentRequestedEventProducer;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(PaymentRiskSaga.class);
        riskAssessmentRequestedEventProducer = mock(RiskAssessmentRequestedEventProducer.class);
        fixture.registerResource(riskAssessmentRequestedEventProducer);
    }

    @Test
    void shouldStartSagaPublishTheRequestAndArmTheTimeout() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(createRiskAssessmentInitiatedEvent())
                .expectActiveSagas(1)
                .expectScheduledDeadlineWithName(Duration.ofMinutes(1), "risk-assessment-timeout");

        verify(riskAssessmentRequestedEventProducer).publishRiskAssessmentRequestedEvent(createRiskAssessmentInitiatedEvent());
    }

    @Test
    void shouldApproveTheFraudCheckAndCancelTheTimeoutOnProceed() {
        RiskAssessment riskAssessment = createProceedAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectActiveSagas(1)
                .expectNoScheduledDeadlines()
                .expectDispatchedCommands(new ApproveFraudCheckCommand(createPaymentId(), riskAssessment));
    }

    @Test
    void shouldRequestManualReviewOnEscalate() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectNoScheduledDeadlines()
                .expectDispatchedCommands(new RequestManualReviewCommand(createPaymentId(), riskAssessment));
    }

    @Test
    void shouldBlockThePaymentOnBlock() {
        RiskAssessment riskAssessment = createBlockAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectNoScheduledDeadlines()
                .expectDispatchedCommands(new BlockPaymentCommand(createPaymentId(), riskAssessment));
    }

    @Test
    void shouldEndSagaWithoutCommandsOnNullAssessment() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEventWithoutAssessment())
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void shouldEndSagaWithoutCommandsOnUnknownAction() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(createRiskAssessment("HOLD", "LOW", 0.50)))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void shouldEndSagaWhenTheAggregateRejectsTheDecision() {
        fixture.setCallbackBehavior((_, _) -> {
            throw new IllegalStateException("Payment is not in FRAUD_CHECK_PENDING status");
        });

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(createProceedAssessment()))
                .expectActiveSagas(0)
                .expectDispatchedCommands(new ApproveFraudCheckCommand(createPaymentId(), createProceedAssessment()));
    }

    @Test
    void shouldNotCancelTheTimeoutTwiceWhenTheCompletionIsRedelivered() {
        RiskAssessment riskAssessment = createProceedAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .andThenAPublished(createRiskAssessmentCompletedEvent(riskAssessment))
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectActiveSagas(1)
                .expectDispatchedCommands(new ApproveFraudCheckCommand(createPaymentId(), riskAssessment));
    }

    @Test
    void shouldExpireTheRiskAssessmentAndEndSagaOnTimeout() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenTimeElapses(PAST_THE_RISK_TIMEOUT)
                .expectActiveSagas(0)
                .expectDispatchedCommands(createExpireRiskAssessmentCommand());
    }

    @Test
    void shouldStillEndSagaWhenExpiringTheAssessmentIsRejected() {
        fixture.setCallbackBehavior((_, _) -> {
            throw new IllegalStateException("aggregate unavailable");
        });

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenTimeElapses(PAST_THE_RISK_TIMEOUT)
                .expectActiveSagas(0)
                .expectDispatchedCommands(createExpireRiskAssessmentCommand());
    }

    @Test
    void shouldEndSagaOnFraudCheckApproved() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createFraudCheckApprovedEvent(createProceedAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndSagaOnManualReviewRequested() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createManualReviewRequestedEvent(createEscalateAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndSagaOnPaymentBlocked() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createPaymentBlockedEvent(createBlockAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndSagaOnPaymentCompleted() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createPaymentCompletedEvent(PaymentStatus.COMPLETED, "done"))
                .expectActiveSagas(0);
    }
}
