package org.banksolution.config;

import org.axonframework.serialization.SerializedObject;
import org.axonframework.serialization.Serializer;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
import com.aml.risk.RiskAction;
import org.banksolution.domain.payment.event.PaymentBlockedEvent;
import org.banksolution.domain.payment.event.RiskAssessmentCompletedEvent;
import org.banksolution.infrastructure.messaging.kafka.mapper.RiskAssessmentMapper;
import org.banksolution.domain.payment.saga.LedgerPostingSaga;
import org.banksolution.domain.payment.saga.PaymentRiskSaga;
import org.banksolution.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEventWithMarl;
import static org.banksolution.fixtures.PaymentFixtures.*;

/**
 * Everything Axon persists between units of work — events, saga state, aggregate snapshots —
 * goes through this serializer; sagas and the aggregate expose no setters, so field access
 * is what keeps their state from silently serialising as {}.
 */
class AxonConfigSerializationTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-26T10:00:00Z");

    private final AxonConfig axonConfig = new AxonConfig();
    private final Serializer serializer = axonConfig.jacksonSerializer(axonConfig.axonObjectMapper());

    @Test
    void shouldRoundTripADomainEventWithNestedValueObjects() {
        PaymentBlockedEvent paymentBlockedEvent =
                createPaymentBlockedEvent(createRiskAssessmentWithMarl("BLOCK", "HIGH", 0.95));

        assertThat(roundTrip(paymentBlockedEvent)).isEqualTo(paymentBlockedEvent);
    }

    @Test
    void shouldKeepTheRiskSagaStateAcrossAReload() throws Exception {
        PaymentRiskSaga paymentRiskSaga = new PaymentRiskSaga();
        setField(paymentRiskSaga, "paymentId", createPaymentId());
        setField(paymentRiskSaga, "deadlineId", "deadline-42");
        setField(paymentRiskSaga, "riskAssessmentCompleted", true);

        PaymentRiskSaga reloadedPaymentRiskSaga = roundTrip(paymentRiskSaga);

        assertThat(getField(reloadedPaymentRiskSaga, "paymentId")).isEqualTo(createPaymentId());
        assertThat(getField(reloadedPaymentRiskSaga, "deadlineId")).isEqualTo("deadline-42");
        assertThat(getField(reloadedPaymentRiskSaga, "riskAssessmentCompleted")).isEqualTo(true);
    }

    @Test
    void shouldKeepTheLedgerPostingSagaStateAcrossAReload() throws Exception {
        LedgerPostingSaga ledgerPostingSaga = new LedgerPostingSaga();
        setField(ledgerPostingSaga, "paymentId", createPaymentId());
        setField(ledgerPostingSaga, "deadlineId", "deadline-7");
        setField(ledgerPostingSaga, "awaitedLedgerPosting", LedgerPostingSaga.AwaitedLedgerPosting.SETTLEMENT);

        LedgerPostingSaga reloadedLedgerPostingSaga = roundTrip(ledgerPostingSaga);

        assertThat(getField(reloadedLedgerPostingSaga, "paymentId")).isEqualTo(createPaymentId());
        assertThat(getField(reloadedLedgerPostingSaga, "deadlineId")).isEqualTo("deadline-7");
        assertThat(getField(reloadedLedgerPostingSaga, "awaitedLedgerPosting"))
                .isEqualTo(LedgerPostingSaga.AwaitedLedgerPosting.SETTLEMENT);
    }

    @Test
    void shouldRestoreTheAggregateFromASnapshotWithItsStatusAndTimestamps() {
        PaymentAggregate paymentAggregate = new PaymentAggregate();
        paymentAggregate.on(createPaymentInitiatedEvent(), OCCURRED_AT);
        paymentAggregate.on(createLedgerAuthorisationInitiatedEvent(), OCCURRED_AT.plusSeconds(1));
        paymentAggregate.on(createRiskAssessmentInitiatedEvent(), OCCURRED_AT.plusSeconds(2));
        paymentAggregate.on(createManualReviewRequestedEvent(createRiskAssessmentWithMarl("ESCALATE", "MEDIUM", 0.6)),
                OCCURRED_AT.plusSeconds(3));

        PaymentAggregate restoredPaymentAggregate = roundTrip(paymentAggregate);

        assertThat(restoredPaymentAggregate.getPaymentId()).isEqualTo(createPaymentId());
        assertThat(restoredPaymentAggregate.getStatus()).isEqualTo(PaymentStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(restoredPaymentAggregate.getAmount()).isEqualByComparingTo(AMOUNT);
        assertThat(restoredPaymentAggregate.isCrossBorderPayment()).isFalse();
        assertThat(restoredPaymentAggregate.getRiskAssessment()).isEqualTo(paymentAggregate.getRiskAssessment());
        assertThat(restoredPaymentAggregate.getInitiatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(restoredPaymentAggregate.getManualReviewRequestedAt()).isEqualTo(OCCURRED_AT.plusSeconds(3));
    }

    @Test
    void shouldNotBakeJdkInternalCollectionTypesIntoStoredEvents() {
        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent = new RiskAssessmentCompletedEvent(
                createPaymentId(),
                RiskAssessmentMapper.toRiskAssessment(createRiskAssessmentCompletedEventWithMarl(RiskAction.BLOCK)));

        String storedPayload = serializer.serialize(riskAssessmentCompletedEvent, String.class).getData();

        assertThat(storedPayload)
                .contains("featureContributions")
                .doesNotContain("java.util.ImmutableCollections")
                .doesNotContain("java.util.Collections$");
        assertThat(roundTrip(riskAssessmentCompletedEvent)).isEqualTo(riskAssessmentCompletedEvent);
    }

    private <T> T roundTrip(T object) {
        SerializedObject<byte[]> serializedObject = serializer.serialize(object, byte[].class);
        return serializer.deserialize(serializedObject);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
