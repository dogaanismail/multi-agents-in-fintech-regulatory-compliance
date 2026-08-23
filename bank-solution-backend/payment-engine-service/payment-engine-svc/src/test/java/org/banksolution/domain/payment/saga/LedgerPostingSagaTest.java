package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.domain.payment.command.DeclineLedgerAuthorisationCommand;
import org.banksolution.domain.payment.command.FailLedgerReleaseCommand;
import org.banksolution.domain.payment.command.FailLedgerSettlementCommand;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.createLedgerAuthorisationInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createLedgerAuthorisedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createLedgerReleaseFailedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createLedgerReleaseInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createLedgerReleasedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createLedgerSettledEvent;
import static org.banksolution.fixtures.PaymentFixtures.createLedgerSettlementInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LedgerPostingSagaTest {

    private static final String TIMEOUT_REASON = "Ledger did not respond within the posting timeout";

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
                .expectActiveSagas(1);

        verify(ledgerPostingRequestedEventProducer).publishAuthorisation(any());
    }

    @Test
    void shouldDeclineTheAuthorisationWhenTheLedgerNeverAnswers() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(3))
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new DeclineLedgerAuthorisationCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldFailTheSettlementWhenTheLedgerNeverAnswers() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerSettlementInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(3))
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new FailLedgerSettlementCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldFailTheReleaseWhenTheLedgerNeverAnswers() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(3))
                .expectActiveSagas(0)
                .expectDispatchedCommands(
                        new FailLedgerReleaseCommand(createPaymentId(), TIMEOUT_REASON));
    }

    @Test
    void shouldNotFireTheTimeoutWhileAwaitingTheComplianceDecision() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .whenTimeElapses(Duration.ofMinutes(30))
                .expectActiveSagas(1)
                .expectNoDispatchedCommands();
    }

    @Test
    void shouldEndTheSagaWhenTheLedgerSettles() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerSettlementInitiatedEvent())
                .whenPublishingA(createLedgerSettledEvent())
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndTheSagaWhenTheLedgerReleases() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenPublishingA(createLedgerReleasedEvent())
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndTheSagaWhenTheReleaseFails() {
        fixture.givenAPublished(createLedgerAuthorisationInitiatedEvent())
                .andThenAPublished(createLedgerAuthorisedEvent())
                .andThenAPublished(createLedgerReleaseInitiatedEvent())
                .whenPublishingA(createLedgerReleaseFailedEvent("Pending authorisation not found"))
                .expectActiveSagas(0);
    }
}
