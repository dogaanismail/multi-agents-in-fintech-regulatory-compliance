package org.banksolution.domain.payment.aggregate;

import org.banksolution.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

/**
 * Sourcing handlers run again on every load, so two replays of the same stream must
 * produce identical state — including timestamps, which therefore come from the events.
 */
class PaymentAggregateReplayTest {

    private static final Instant FIRST_EVENT_AT = Instant.parse("2026-08-26T10:00:00Z");
    private static final Instant SECOND_EVENT_AT = Instant.parse("2026-08-26T10:00:01Z");

    @Test
    void shouldReproduceTheSameTimestampsOnEveryReplay() {
        PaymentAggregate firstReplay = replayInitiatedAndAuthorisationPending();
        PaymentAggregate secondReplay = replayInitiatedAndAuthorisationPending();

        assertThat(firstReplay.getInitiatedAt()).isEqualTo(FIRST_EVENT_AT).isEqualTo(secondReplay.getInitiatedAt());
        assertThat(firstReplay.getLedgerAuthorisationInitiatedAt())
                .isEqualTo(SECOND_EVENT_AT)
                .isEqualTo(secondReplay.getLedgerAuthorisationInitiatedAt());
        assertThat(firstReplay.getStatus()).isEqualTo(PaymentStatus.AUTHORISATION_PENDING);
    }

    @Test
    void shouldStampEachLifecycleFieldFromItsOwnEvent() {
        PaymentAggregate paymentAggregate = replayInitiatedAndAuthorisationPending();

        paymentAggregate.on(createRiskAssessmentInitiatedEvent(), FIRST_EVENT_AT.plusSeconds(2));
        paymentAggregate.on(createRiskAssessmentCompletedEvent(createEscalateAssessment()), FIRST_EVENT_AT.plusSeconds(3));
        assertThat(paymentAggregate.getRiskAssessmentCompletedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(3));
        assertThat(paymentAggregate.getRiskAssessment()).isEqualTo(createEscalateAssessment());
        paymentAggregate.on(createManualReviewRequestedEvent(createEscalateAssessment()), FIRST_EVENT_AT.plusSeconds(4));
        paymentAggregate.on(createLedgerSettlementInitiatedEvent(), FIRST_EVENT_AT.plusSeconds(5));
        paymentAggregate.on(createLedgerReleaseInitiatedEvent(), FIRST_EVENT_AT.plusSeconds(6));
        paymentAggregate.on(createDecisionOverriddenEvent(true, "BLOCKED"), FIRST_EVENT_AT.plusSeconds(7));
        paymentAggregate.on(createPaymentCompletedEvent(PaymentStatus.COMPLETED, "done"), FIRST_EVENT_AT.plusSeconds(8));

        assertThat(paymentAggregate.getRiskAssessmentRequestedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(2));
        assertThat(paymentAggregate.getRiskAssessmentCompletedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(4));
        assertThat(paymentAggregate.getManualReviewRequestedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(4));
        assertThat(paymentAggregate.getLedgerSettlementInitiatedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(5));
        assertThat(paymentAggregate.getLedgerReleaseInitiatedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(6));
        assertThat(paymentAggregate.getDecisionOverriddenAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(7));
        assertThat(paymentAggregate.getCompletedAt()).isEqualTo(FIRST_EVENT_AT.plusSeconds(8));
        assertThat(paymentAggregate.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    private static PaymentAggregate replayInitiatedAndAuthorisationPending() {
        PaymentAggregate paymentAggregate = new PaymentAggregate();
        paymentAggregate.on(createPaymentInitiatedEvent(), FIRST_EVENT_AT);
        paymentAggregate.on(createLedgerAuthorisationInitiatedEvent(), SECOND_EVENT_AT);
        return paymentAggregate;
    }
}
