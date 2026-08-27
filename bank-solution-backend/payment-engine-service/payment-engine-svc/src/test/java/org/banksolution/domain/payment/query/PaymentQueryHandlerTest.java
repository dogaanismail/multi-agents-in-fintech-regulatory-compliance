package org.banksolution.domain.payment.query;

import org.axonframework.eventsourcing.EventSourcedAggregate;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.modelling.command.LockAwareAggregate;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.banksolution.enums.PaymentType;
import org.banksolution.exception.PaymentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryHandlerTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-26T10:00:00Z");

    @Mock
    private EventSourcingRepository<PaymentAggregate> paymentRepository;

    @InjectMocks
    private PaymentQueryHandler paymentQueryHandler;

    @Test
    @SuppressWarnings("unchecked")
    void shouldProjectTheLoadedAggregateIntoAResponse() {
        PaymentAggregate paymentAggregate = new PaymentAggregate();
        paymentAggregate.on(createPaymentInitiatedEvent(), OCCURRED_AT);
        paymentAggregate.on(createLedgerAuthorisationInitiatedEvent(), OCCURRED_AT.plusSeconds(1));
        paymentAggregate.on(createRiskAssessmentInitiatedEvent(), OCCURRED_AT.plusSeconds(2));
        paymentAggregate.on(createManualReviewRequestedEvent(createEscalateAssessment()), OCCURRED_AT.plusSeconds(3));
        LockAwareAggregate<PaymentAggregate, EventSourcedAggregate<PaymentAggregate>> lockAwareAggregate = mock(LockAwareAggregate.class);
        EventSourcedAggregate<PaymentAggregate> eventSourcedAggregate = mock(EventSourcedAggregate.class);
        when(paymentRepository.load(PAYMENT_UUID.toString())).thenReturn(lockAwareAggregate);
        when(lockAwareAggregate.getWrappedAggregate()).thenReturn(eventSourcedAggregate);
        when(eventSourcedAggregate.getAggregateRoot()).thenReturn(paymentAggregate);
        when(lockAwareAggregate.version()).thenReturn(3L);

        PaymentResponse paymentResponse = paymentQueryHandler.handle(new FindPaymentQuery(PAYMENT_UUID.toString()));

        assertThat(paymentResponse.paymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(paymentResponse.referenceNumber()).isEqualTo("PAY-11111111");
        assertThat(paymentResponse.customerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(paymentResponse.sourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(paymentResponse.destinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
        assertThat(paymentResponse.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(paymentResponse.convertedAmount()).isEqualByComparingTo(CONVERTED_AMOUNT);
        assertThat(paymentResponse.appliedExchangeRate()).isEqualByComparingTo(EXCHANGE_RATE);
        assertThat(paymentResponse.paymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(paymentResponse.paymentScheme()).isEqualTo(PAYMENT_SCHEME);
        assertThat(paymentResponse.status()).isEqualTo(PaymentStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(paymentResponse.fraudStatus()).isEqualTo(FraudAnalysisStatus.REVIEW_REQUIRED);
        assertThat(paymentResponse.riskAssessment()).isEqualTo(createEscalateAssessment());
        assertThat(paymentResponse.initiatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(paymentResponse.ledgerAuthorisationInitiatedAt()).isEqualTo(OCCURRED_AT.plusSeconds(1));
        assertThat(paymentResponse.riskAssessmentRequestedAt()).isEqualTo(OCCURRED_AT.plusSeconds(2));
        assertThat(paymentResponse.manualReviewRequestedAt()).isEqualTo(OCCURRED_AT.plusSeconds(3));
        assertThat(paymentResponse.completedAt()).isNull();
        assertThat(paymentResponse.version()).isEqualTo(3L);
    }

    @Test
    void shouldReportAMissingPaymentWhenTheAggregateCannotBeLoaded() {
        when(paymentRepository.load(PAYMENT_UUID.toString())).thenThrow(new IllegalStateException("no events"));
        FindPaymentQuery findPaymentQuery = new FindPaymentQuery(PAYMENT_UUID.toString());

        assertThatThrownBy(() -> paymentQueryHandler.handle(findPaymentQuery))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Failed to load for paymentId: " + PAYMENT_UUID)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
