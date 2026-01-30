package org.banksolution.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryGateway;
import org.banksolution.domain.payment.query.FindPaymentQuery;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentQueryService {

    private final QueryGateway queryGateway;

    public PaymentResponse findPaymentById(PaymentId paymentId) {
        log.debug("Querying payment for paymentId: {}", paymentId);
        return queryGateway
                .query(new FindPaymentQuery(paymentId.toString()), PaymentResponse.class)
                .join();
    }
}
