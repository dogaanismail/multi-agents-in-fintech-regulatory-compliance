package org.banksolution.domain.payment.saga;

import org.axonframework.test.saga.SagaTestFixture;
import org.banksolution.domain.payment.event.AccountChargeInitiatedEvent;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.infrastructure.messaging.kafka.producer.AccountChargeRequestedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.banksolution.fixtures.PaymentFixtures.createAccountChargeFailedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createAccountChargeInitiatedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createAccountChargedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createBlockAssessment;
import static org.banksolution.fixtures.PaymentFixtures.createConfirmAccountChargedCommand;
import static org.banksolution.fixtures.PaymentFixtures.createFailAccountChargeCommand;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentBlockedEvent;
import static org.banksolution.fixtures.PaymentFixtures.createPaymentCompletedEvent;
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
    void shouldStartSagaAndPublishAccountChargeRequest() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(createAccountChargeInitiatedEvent())
                .expectActiveSagas(1);

        verify(accountChargeRequestedEventProducer).publishAccountChargeRequestedEvent(any(AccountChargeInitiatedEvent.class));
    }

    @Test
    void shouldDispatchConfirmCommandOnAccountCharged() {
        fixture.givenAPublished(createAccountChargeInitiatedEvent())
                .whenPublishingA(createAccountChargedEvent())
                .expectActiveSagas(1)
                .expectDispatchedCommands(createConfirmAccountChargedCommand());
    }

    @Test
    void shouldDispatchFailAccountChargeOnTimeout() {
        fixture.givenAPublished(createAccountChargeInitiatedEvent())
                .whenTimeElapses(Duration.ofMinutes(3))
                .expectActiveSagas(1)
                .expectDispatchedCommands(createFailAccountChargeCommand("Account charge timeout after 2 minutes"));
    }

    @Test
    void shouldEndSagaOnPaymentCompleted() {
        fixture.givenAPublished(createAccountChargeInitiatedEvent())
                .whenPublishingA(createPaymentCompletedEvent(PaymentStatus.COMPLETED, "Payment successfully processed and account charged"))
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndSagaOnPaymentBlocked() {
        fixture.givenAPublished(createAccountChargeInitiatedEvent())
                .whenPublishingA(createPaymentBlockedEvent(createBlockAssessment()))
                .expectActiveSagas(0);
    }

    @Test
    void shouldEndSagaOnAccountChargeFailed() {
        fixture.givenAPublished(createAccountChargeInitiatedEvent())
                .whenPublishingA(createAccountChargeFailedEvent("Insufficient funds"))
                .expectActiveSagas(0);
    }
}
