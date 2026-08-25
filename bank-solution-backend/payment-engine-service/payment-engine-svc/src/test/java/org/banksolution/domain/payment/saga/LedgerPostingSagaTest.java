package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.domain.payment.command.DeclineLedgerAuthorisationCommand;
import org.banksolution.domain.payment.command.FailLedgerReleaseCommand;
import org.banksolution.domain.payment.command.FailLedgerSettlementCommand;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LedgerPostingSagaTest {

    private static final String TIMEOUT_REASON = "Ledger did not respond within the posting timeout";
    private static final Duration PAST_THE_POSTING_TIMEOUT = Duration.ofMinutes(3);

    private SagaTestFixture<LedgerPostingSaga> fixture;
    private LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(LedgerPostingSaga.class);
        ledgerPostingRequestedEventProducer = mock(LedgerPostingRequestedEventProducer.class);
        fixture.registerResource(ledgerPostingRequestedEventProducer);
    }

    @Test
    void shouldPublishTheAuthorisationRequestWhenTheSagaStarts() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(createLedgerAuthorisationInitiatedEvent())
                .expectActiveSagas(1)
                .expectScheduledDeadlineWithName(Duration.ofMinutes(2), "ledger-posting-timeout");

        verify(ledgerPostingRequestedEventProducer).publishAuthorisation(createLedgerAuthorisationInitiatedEvent());
    }

    @Test
    void shouldPublishTheSettlementRequestAndRearmTheTimeout() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .whenPublishingA(createLedgerSettlementInitiatedEvent())
                .expectActiveSagas(1)
                .expectScheduledDeadlineWithName(Duration.ofMinutes(2), "ledger-posting-timeout");

        verify(ledgerPostingRequestedEventProducer).publishSettlement(createPaymentId());
    }

    @Test
    void shouldPublishTheReleaseRequestAndRearmTheTimeout() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .whenPublishingA(createLedgerReleaseInitiatedEvent())
                .expectActiveSagas(1)
                .expectScheduledDeadlineWithName(Duration.ofMinutes(2), "ledger-posting-timeout");

        verify(ledgerPostingRequestedEventProducer).publishRelease(createPaymentId());
    }

    @Test
    void shouldDeclineTheAuthorisationWhenTheLedgerNeverAnswers() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenTimeElapses(PAST_THE_POSTING_TIMEOUT)
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new DeclineLedgerAuthorisationCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldFailTheSettlementWhenTheLedgerNeverAnswers() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerSettlementInitiatedEvent())
                .whenTimeElapses(PAST_THE_POSTING_TIMEOUT)
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new FailLedgerSettlementCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldFailTheReleaseWhenTheLedgerNeverAnswers() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenTimeElapses(PAST_THE_POSTING_TIMEOUT)
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new FailLedgerReleaseCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldStillEndTheSagaWhenFailingTheTimedOutPostingIsRejected() {
        fixture.setCallbackBehavior((_, _) -> {
            throw new IllegalStateException("aggregate already moved on");
        });

        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenTimeElapses(PAST_THE_POSTING_TIMEOUT)
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new DeclineLedgerAuthorisationCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldNotFireTheTimeoutWhileAwaitingTheComplianceDecision() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .whenTimeElapses(Duration.ofMinutes(30))
                .expectActiveSagas(1)
                .expectNoScheduledDeadlines()
                .expectNoDispatchedCommands();
    }

    @Test
    void shouldTolerateARedeliveredAuthorisationWithNoTimeoutLeftToCancel() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .whenPublishingA(createLedgerAuthorisedEvent())
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }

    @Test
    void shouldEndTheSagaWhenTheLedgerDeclinesTheAuthorisation() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenPublishingA(createLedgerAuthorisationDeclinedEvent("Insufficient funds"))
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }

    @Test
    void shouldEndTheSagaWhenTheLedgerSettles() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerSettlementInitiatedEvent())
                .whenPublishingA(createLedgerSettledEvent())
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }

    @Test
    void shouldEndTheSagaWhenTheSettlementFails() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerSettlementInitiatedEvent())
                .whenPublishingA(createLedgerSettlementFailedEvent("Pending transfer expired"))
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }

    @Test
    void shouldEndTheSagaWhenTheLedgerReleases() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenPublishingA(createLedgerReleasedEvent())
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }

    @Test
    void shouldEndTheSagaWhenTheReleaseFails() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenPublishingA(createLedgerReleaseFailedEvent("Pending authorisation not found"))
                .expectActiveSagas(0)
                .expectNoScheduledDeadlines();
    }
}
