package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.config.LedgerPostingTimeoutProperties;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LedgerPostingSagaTest {

    private static final String LEDGER_POSTING_TIMEOUT_DEADLINE = "ledger-posting-timeout";
    private static final Duration INITIAL_TIMEOUT = Duration.ofMinutes(2);

    private SagaTestFixture<LedgerPostingSaga> fixture;
    private LedgerPostingRequestedEventProducer ledgerPostingRequestedEventProducer;
    private LedgerPostingTimeoutProperties ledgerPostingTimeoutProperties;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(LedgerPostingSaga.class);
        ledgerPostingRequestedEventProducer = mock(LedgerPostingRequestedEventProducer.class);
        ledgerPostingTimeoutProperties = new LedgerPostingTimeoutProperties();
        fixture.registerResource(ledgerPostingRequestedEventProducer);
        fixture.registerResource(ledgerPostingTimeoutProperties);
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
    void shouldResendTheAuthorisationInsteadOfDecliningWhenTheLedgerHasNotAnswered() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenTimeElapses(INITIAL_TIMEOUT)
                .expectActiveSagas(1)
                .expectNoDispatchedCommands()
                .expectScheduledDeadlineWithName(Duration.ofMinutes(4), LEDGER_POSTING_TIMEOUT_DEADLINE);

        verify(ledgerPostingRequestedEventProducer, times(2)).publishAuthorisation(createLedgerAuthorisationInitiatedEvent());
    }

    @Test
    void shouldResendTheSettlementInsteadOfFailingItWhenTheLedgerHasNotAnswered() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerSettlementInitiatedEvent())
                .whenTimeElapses(INITIAL_TIMEOUT)
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();

        verify(ledgerPostingRequestedEventProducer, times(2)).publishSettlement(createPaymentId());
    }

    @Test
    void shouldResendTheReleaseInsteadOfFailingItWhenTheLedgerHasNotAnswered() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenTimeElapses(INITIAL_TIMEOUT)
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();

        verify(ledgerPostingRequestedEventProducer, times(2)).publishRelease(createPaymentId());
    }

    @Test
    void shouldDoubleTheWaitBetweenResendsUpToTheMaximumTimeout() throws Exception {
        ledgerPostingTimeoutProperties.setMaxTimeout(Duration.ofMinutes(5));

        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenTimeElapses(Duration.ofMinutes(2))
                .andThenTimeElapses(Duration.ofMinutes(4))
                .whenTimeElapses(Duration.ofMinutes(5))
                .expectScheduledDeadlineWithName(Duration.ofMinutes(5), LEDGER_POSTING_TIMEOUT_DEADLINE);

        verify(ledgerPostingRequestedEventProducer, times(4)).publishAuthorisation(createLedgerAuthorisationInitiatedEvent());
    }

    @Test
    void shouldStopResendingButKeepThePaymentPendingOnceTheResendsAreExhausted() {
        ledgerPostingTimeoutProperties.setMaxResends(2);

        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenTimeElapses(Duration.ofHours(2))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands()
                .expectNoScheduledDeadlines();

        verify(ledgerPostingRequestedEventProducer, times(3)).publishAuthorisation(createLedgerAuthorisationInitiatedEvent());
    }

    @Test
    void shouldKeepTheTimeoutArmedWhenAResendCannotReachKafka() {
        doNothing()
                .doThrow(new IllegalStateException("broker unavailable"))
                .when(ledgerPostingRequestedEventProducer).publishAuthorisation(createLedgerAuthorisationInitiatedEvent());

        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenTimeElapses(INITIAL_TIMEOUT)
                .expectActiveSagas(1)
                .expectNoDispatchedCommands()
                .expectScheduledDeadlineWithName(Duration.ofMinutes(4), LEDGER_POSTING_TIMEOUT_DEADLINE);
    }

    @Test
    void shouldAcceptALateLedgerOutcomeAfterAResend() throws Exception {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenTimeElapses(INITIAL_TIMEOUT)
                .whenPublishingA(createLedgerAuthorisedEvent())
                .expectActiveSagas(1)
                .expectNoScheduledDeadlines()
                .expectNoDispatchedCommands();
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
