package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.domain.payment.event.AccountChargeInitiatedEvent;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.infrastructure.messaging.kafka.producer.AccountChargeRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.accountChargeFailedEvent;
import static org.banksolution.fixtures.PaymentFixtures.accountChargeInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.accountChargedEvent;
import static org.banksolution.fixtures.PaymentFixtures.blockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.confirmAccountChargedCommand;
import static org.banksolution.fixtures.PaymentFixtures.failAccountChargeCommand;
import static org.banksolution.fixtures.PaymentFixtures.paymentBlockedEvent;
import static org.banksolution.fixtures.PaymentFixtures.paymentCompletedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountChargeSagaTest {

    private SagaTestFixture<AccountChargeSaga> fixture;
    private AccountChargeRequestedEventProducer accountChargeRequestedEventProducer;

    @BeforeEach
    void setUp() {
        fixture = new SagaTestFixture<>(AccountChargeSaga.class);
        accountChargeRequestedEventProducer = mock(AccountChargeRequestedEventProducer.class);
        fixture.registerResource(accountChargeRequestedEventProducer);
    }

    @Test
    void startsSagaAndPublishesAccountChargeRequest() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(accountChargeInitiatedEvent())
                .expectActiveSagas(1);

        verify(accountChargeRequestedEventProducer).publishAccountChargeRequestedEvent(any(AccountChargeInitiatedEvent.class));
    }

    @Test
    void accountChargedDispatchesConfirmCommand() {
        fixture.givenAPublished(accountChargeInitiatedEvent())
                .whenPublishingA(accountChargedEvent())
                .expectActiveSagas(1)
                .expectDispatchedCommands(confirmAccountChargedCommand());
    }

    @Test
    void timeoutDispatchesFailAccountCharge() {
        fixture.givenAPublished(accountChargeInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(3))
                .expectActiveSagas(1)
                .expectDispatchedCommands(failAccountChargeCommand("Account charge timeout after 2 minutes"));
    }

    @Test
    void paymentCompletedEndsSaga() {
        fixture.givenAPublished(accountChargeInitiatedEvent())
                .whenPublishingA(paymentCompletedEvent(PaymentStatus.COMPLETED, "Payment successfully processed and account charged"))
                .expectActiveSagas(0);
    }

    @Test
    void paymentBlockedEndsSaga() {
        fixture.givenAPublished(accountChargeInitiatedEvent())
                .whenPublishingA(paymentBlockedEvent(blockAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void accountChargeFailedEndsSaga() {
        fixture.givenAPublished(accountChargeInitiatedEvent())
                .whenPublishingA(accountChargeFailedEvent("Insufficient funds"))
                .expectActiveSagas(0);
    }
}
