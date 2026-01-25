package org.banksolution.domain.payment.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.banksolution.domain.payment.aggregate.PaymentAggregate;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.banksolution.enums.PaymentType;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentQueryHandler {

    private final EventSourcingRepository<PaymentAggregate> paymentRepository;

    @QueryHandler
    public PaymentResponse handle(FindPaymentQuery query) {
        log.debug("Handling FindPaymentQuery for paymentId: {}", query.paymentId());
        
        try {
            var aggregate = paymentRepository.load(query.paymentId()).getWrappedAggregate().getAggregateRoot();
            
            return new PaymentResponse(
                    aggregate.getPaymentId().toString(),
                    aggregate.getReferenceNumber(),
                    aggregate.getCustomerId().toString(),
                    aggregate.getSourceAccountId().toString(),
                    aggregate.getDestinationAccountId().toString(),
                    aggregate.getAmount(),
                    aggregate.getCurrency(),
                    PaymentType.valueOf(aggregate.getPaymentType()),
                    aggregate.getDescription(),
                    aggregate.getStatus(),
                    aggregate.getFraudStatus(),
                    aggregate.getRiskAssessment(),
                    aggregate.getVersion(),
                    aggregate.getInitiatedAt(),
                    aggregate.getRiskAssessmentRequestedAt(),
                    aggregate.getRiskAssessmentCompletedAt(),
                    aggregate.getFraudCheckApprovedAt(),
                    aggregate.getManualReviewRequestedAt(),
                    aggregate.getManualReviewApprovedAt(),
                    aggregate.getManualReviewRejectedAt(),
                    aggregate.getAccountChargeInitiatedAt(),
                    aggregate.getAccountChargedAt(),
                    aggregate.getAccountChargeFailedAt(),
                    aggregate.getCompletedAt(),
                    aggregate.getBlockedAt(),
                    aggregate.getManualReviewedBy(),
                    aggregate.getManualReviewNotes(),
                    aggregate.getBlockReason(),
                    aggregate.getFailureReason()
            );
        } catch (Exception e) {
            log.error("Failed to load payment aggregate: {}", query.paymentId(), e);
            throw new IllegalStateException("Payment not found: " + query.paymentId(), e);
        }
    }
}
