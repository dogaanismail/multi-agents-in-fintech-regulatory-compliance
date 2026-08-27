package org.banksolution.domain.payment.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.banksolution.enums.PaymentType;
import org.banksolution.exception.PaymentNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final EventSourcingRepository<PaymentAggregate> paymentRepository;

    @QueryHandler
    public PaymentResponse handle(FindPaymentQuery findPaymentQuery) {
        log.debug("Handling FindPaymentQuery for paymentId: {}", findPaymentQuery.paymentId());

        try {
            // Event-sourced aggregates never populate an @AggregateVersion field; the version
            // is the last event's sequence number, held by the wrapper.
            var loadedAggregate = paymentRepository.load(findPaymentQuery.paymentId());
            PaymentAggregate paymentAggregate = loadedAggregate.getWrappedAggregate().getAggregateRoot();
            Long aggregateVersion = loadedAggregate.version();

            return new PaymentResponse(
                    paymentAggregate.getPaymentId().toString(),
                    paymentAggregate.getReferenceNumber(),
                    paymentAggregate.getCustomerId().toString(),
                    paymentAggregate.getSourceAccountId().toString(),
                    paymentAggregate.getDestinationAccountId().toString(),
                    paymentAggregate.getAmount(),
                    paymentAggregate.getFromCurrency(),
                    paymentAggregate.getToCurrency(),
                    paymentAggregate.getConvertedAmount(),
                    paymentAggregate.getAppliedExchangeRate(),
                    PaymentType.valueOf(paymentAggregate.getPaymentType()),
                    paymentAggregate.getPaymentScheme(),
                    paymentAggregate.getDescription(),
                    paymentAggregate.isCrossBorderPayment(),
                    paymentAggregate.getStatus(),
                    paymentAggregate.getFraudStatus(),
                    paymentAggregate.getRiskAssessment(),
                    aggregateVersion,
                    paymentAggregate.getInitiatedAt(),
                    paymentAggregate.getRiskAssessmentRequestedAt(),
                    paymentAggregate.getRiskAssessmentCompletedAt(),
                    paymentAggregate.getFraudCheckApprovedAt(),
                    paymentAggregate.getManualReviewRequestedAt(),
                    paymentAggregate.getManualReviewApprovedAt(),
                    paymentAggregate.getManualReviewRejectedAt(),
                    paymentAggregate.getLedgerAuthorisationInitiatedAt(),
                    paymentAggregate.getLedgerAuthorisedAt(),
                    paymentAggregate.getLedgerSettlementInitiatedAt(),
                    paymentAggregate.getLedgerSettledAt(),
                    paymentAggregate.getLedgerReleaseInitiatedAt(),
                    paymentAggregate.getLedgerReleasedAt(),
                    paymentAggregate.getCompletedAt(),
                    paymentAggregate.getBlockedAt(),
                    paymentAggregate.getManualReviewedBy(),
                    paymentAggregate.getManualReviewNotes(),
                    paymentAggregate.getBlockReason(),
                    paymentAggregate.getFailureReason(),
                    paymentAggregate.getDecisionOverriddenBy(),
                    paymentAggregate.getDecisionOverrideReason(),
                    paymentAggregate.getDecisionOverriddenAt());
        } catch (Exception exception) {
            log.error("Failed to load payment aggregate: {}", findPaymentQuery.paymentId(), exception);
            throw new PaymentNotFoundException("Failed to load for paymentId: %s", exception, findPaymentQuery.paymentId());
        }
    }
}
