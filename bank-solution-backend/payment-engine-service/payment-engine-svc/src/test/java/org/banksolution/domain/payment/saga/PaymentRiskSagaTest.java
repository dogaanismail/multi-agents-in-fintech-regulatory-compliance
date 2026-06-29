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

import static org.banksolution.fixtures.PaymentFixtures.blockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.escalateAssessment;
import static org.banksolution.fixtures.PaymentFixtures.fraudCheckApprovedEvent;
import static org.banksolution.fixtures.PaymentFixtures.manualReviewRequestedEvent;
import static org.banksolution.fixtures.PaymentFixtures.paymentBlockedEvent;
import static org.banksolution.fixtures.PaymentFixtures.paymentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.paymentId;
import static org.banksolution.fixtures.PaymentFixtures.proceedAssessment;
import static org.banksolution.fixtures.PaymentFixtures.riskAssessment;
import static org.banksolution.fixtures.PaymentFixtures.riskAssessmentCompletedEvent;
import static org.banksolution.fixtures.PaymentFixtures.riskAssessmentCompletedEventWithoutAssessment;
import static org.banksolution.fixtures.PaymentFixtures.riskAssessmentInitiatedEvent;
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
    void startsSagaAndPublishesRiskAssessmentRequest() {
        RiskAssessmentInitiatedEvent event = riskAssessmentInitiatedEvent();

        fixture.givenNoPriorActivity()
                .whenPublishingA(event)
                .expectActiveSagas(1);

        verify(riskAssessmentRequestedEventProducer).publishRiskAssessmentRequestedEvent(any(RiskAssessmentInitiatedEvent.class));
    }

    @Test
    void proceedActionDispatchesApproveFraudCheck() {
        RiskAssessment riskAssessment = proceedAssessment();

        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(riskAssessmentCompletedEvent(riskAssessment))
                .expectActiveSagas(1)
                .expectDispatchedCommands(new ApproveFraudCheckCommand(paymentId(), riskAssessment));
    }

    @Test
    void escalateActionDispatchesRequestManualReview() {
        RiskAssessment riskAssessment = escalateAssessment();

        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(riskAssessmentCompletedEvent(riskAssessment))
                .expectDispatchedCommands(new RequestManualReviewCommand(paymentId(), riskAssessment));
    }

    @Test
    void blockActionDispatchesBlockPayment() {
        RiskAssessment riskAssessment = blockAssessment();

        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(riskAssessmentCompletedEvent(riskAssessment))
                .expectDispatchedCommands(new BlockPaymentCommand(paymentId(), riskAssessment));
    }

    @Test
    void nullAssessmentEndsSagaWithoutCommands() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(riskAssessmentCompletedEventWithoutAssessment())
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void unknownActionEndsSagaWithoutCommands() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(riskAssessmentCompletedEvent(riskAssessment("HOLD", "LOW", 0.50)))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void timeoutEndsSagaWithoutDispatchingCommands() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(2))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void fraudCheckApprovedEndsSaga() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(fraudCheckApprovedEvent(proceedAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void manualReviewRequestedEndsSaga() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(manualReviewRequestedEvent(escalateAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void paymentBlockedEndsSaga() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(paymentBlockedEvent(blockAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void paymentCompletedEndsSaga() {
        fixture.givenAPublished(riskAssessmentInitiatedEvent())
                .whenPublishingA(paymentCompletedEvent(PaymentStatus.COMPLETED, "done"))
                .expectActiveSagas(0);
    }
}
