package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.domain.payment.command.ApproveFraudCheckCommand;
import org.banksolution.domain.payment.command.BlockPaymentCommand;
import org.banksolution.domain.payment.command.RequestManualReviewCommand;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskAssessmentRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.createBlockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createEscalateAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createFraudCheckApprovedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createManualReviewRequestedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentBlockedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentId;
import static org.banksolution.fixtures.PaymentFixtures.createProceedAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createRiskAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createRiskAssessmentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createRiskAssessmentCompletedEventWithoutAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createRiskAssessmentInitiatedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentRiskSagaTest {

    private SagaTestFixture<PaymentRiskSaga> fixture;
    private RiskAssessmentRequestedEventProducer riskAssessmentRequestedEventProducer;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(PaymentRiskSaga.class);
        riskAssessmentRequestedEventProducer = mock(RiskAssessmentRequestedEventProducer.class);
        fixture.registerResource(riskAssessmentRequestedEventProducer);
    }

    @Test
    void shouldStartSagaAndPublishRiskAssessmentRequest() {
        RiskAssessmentInitiatedEvent event = createRiskAssessmentInitiatedEvent();

        fixture.givenNoPriorActivity()
                .whenPublishingA(event)
                .expectActiveSagas(1);

        verify(riskAssessmentRequestedEventProducer).publishRiskAssessmentRequestedEvent(any(RiskAssessmentInitiatedEvent.class));
    }

    @Test
    void shouldDispatchApproveFraudCheckOnProceedAction() {
        RiskAssessment riskAssessment = createProceedAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectActiveSagas(1)
                .expectDispatchedCommands(new ApproveFraudCheckCommand(createPaymentId(), riskAssessment));
    }

    @Test
    void shouldDispatchRequestManualReviewOnEscalateAction() {
        RiskAssessment riskAssessment = createEscalateAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
                .expectDispatchedCommands(new RequestManualReviewCommand(createPaymentId(), riskAssessment));
    }

    @Test
    void shouldDispatchBlockPaymentOnBlockAction() {
        RiskAssessment riskAssessment = createBlockAssessment();

        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenPublishingA(createRiskAssessmentCompletedEvent(riskAssessment))
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
    void shouldEndSagaWithoutDispatchingCommandsOnTimeout() {
        fixture.givenAPublished(createRiskAssessmentInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(2))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
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
