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
    public PaymentResponse handle(FindPaymentQuery query) {
        log.debug("Handling FindPaymentQuery for paymentId: {}", query.paymentId());

        try {
            PaymentAggregate paymentAggregate = paymentRepository.load(query.paymentId()).getWrappedAggregate().getAggregateRoot();

            return new PaymentResponse(
                    paymentAggregate.getPaymentId().toString(),
                    paymentAggregate.getReferenceNumber(),
                    paymentAggregate.getCustomerId().toString(),
                    paymentAggregate.getSourceAccountId().toString(),
                    paymentAggregate.getDestinationAccountId().toString(),
                    paymentAggregate.getAmount(),
                    paymentAggregate.getCurrency(),
                    PaymentType.valueOf(paymentAggregate.getPaymentType()),
                    paymentAggregate.getDescription(),
                    paymentAggregate.isCrossBorderPayment(),
                    paymentAggregate.getStatus(),
                    paymentAggregate.getFraudStatus(),
                    paymentAggregate.getRiskAssessment(),
                    paymentAggregate.getVersion(),
                    paymentAggregate.getInitiatedAt(),
                    paymentAggregate.getRiskAssessmentRequestedAt(),
                    paymentAggregate.getRiskAssessmentCompletedAt(),
                    paymentAggregate.getFraudCheckApprovedAt(),
                    paymentAggregate.getManualReviewRequestedAt(),
                    paymentAggregate.getManualReviewApprovedAt(),
                    paymentAggregate.getManualReviewRejectedAt(),
                    paymentAggregate.getAccountChargeInitiatedAt(),
                    paymentAggregate.getAccountChargedAt(),
                    paymentAggregate.getAccountChargeFailedAt(),
                    paymentAggregate.getCompletedAt(),
                    paymentAggregate.getBlockedAt(),
                    paymentAggregate.getManualReviewedBy(),
                    paymentAggregate.getManualReviewNotes(),
                    paymentAggregate.getBlockReason(),
                    paymentAggregate.getFailureReason());
        } catch (Exception e) {
            log.error("Failed to load payment aggregate: {}", query.paymentId(), e);
            throw new PaymentNotFoundException("Failed to load for paymentId: %s", e, query.paymentId());
        }
    }
}
