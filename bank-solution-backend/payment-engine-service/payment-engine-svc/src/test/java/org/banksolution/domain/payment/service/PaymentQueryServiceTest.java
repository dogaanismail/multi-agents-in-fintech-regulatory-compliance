package org.banksolution.domain.payment.service;

import org.axonframework.queryhandling.QueryGateway;
import org.banksolution.domain.payment.query.FindPaymentQuery;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceTest {

    @Mock
    private QueryGateway queryGateway;

    @InjectMocks
    private PaymentQueryService paymentQueryService;

    @Test
    void shouldQueryThePaymentByItsIdentifierAndWaitForTheAnswer() {
        PaymentResponse expectedPaymentResponse =
                createPaymentResponse(PaymentStatus.COMPLETED, FraudAnalysisStatus.APPROVED, createProceedAssessment());
        when(queryGateway.query(new FindPaymentQuery(PAYMENT_UUID.toString()), PaymentResponse.class))
                .thenReturn(CompletableFuture.completedFuture(expectedPaymentResponse));

        PaymentResponse paymentResponse = paymentQueryService.findPaymentById(createPaymentId());

        assertThat(paymentResponse).isSameAs(expectedPaymentResponse);
    }
}
