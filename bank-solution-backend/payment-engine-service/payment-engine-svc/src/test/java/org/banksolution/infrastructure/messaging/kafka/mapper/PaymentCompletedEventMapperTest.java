package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.PaymentCompletedEvent;
import com.aml.payment.PaymentType;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.valueobject.RiskAssessment;
import org.banksolution.enums.FraudAnalysisStatus;
import org.banksolution.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class PaymentCompletedEventMapperTest {

    @Test
    void shouldReportASettledPaymentAsHavingPassedTheRiskCheck() {
        PaymentResponse paymentResponse =
                createPaymentResponse(PaymentStatus.COMPLETED, FraudAnalysisStatus.APPROVED, createProceedAssessment());

        PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEventMapper.toAvroEvent(paymentResponse);

        assertThat(paymentCompletedEvent.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(paymentCompletedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(paymentCompletedEvent.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(paymentCompletedEvent.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
        assertThat(paymentCompletedEvent.getSourceAccountBankLocation()).isNull();
        assertThat(paymentCompletedEvent.getAmount()).isEqualTo("100.00");
        assertThat(paymentCompletedEvent.getFromCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(paymentCompletedEvent.getToCurrency()).isEqualTo(TO_CURRENCY);
        assertThat(paymentCompletedEvent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(paymentCompletedEvent.getRiskCheckPassed()).isTrue();
        assertThat(paymentCompletedEvent.getRiskScore()).isEqualTo(0.10);
        assertThat(paymentCompletedEvent.getProcessingTimeMs()).isEqualTo(5000.0);
        assertThat(paymentCompletedEvent.getEventId()).isNotBlank();
    }

    @Test
    void shouldReportABlockedPaymentAsHavingFailedTheRiskCheckWithoutAScoreWhenNoneIsKnown() {
        RiskAssessment riskAssessmentWithoutScore = new RiskAssessment("risk-req-3", null, "HIGH", "BLOCK", List.of(), null, 1L, null);
        PaymentResponse paymentResponse =
                createPaymentResponse(PaymentStatus.BLOCKED, FraudAnalysisStatus.BLOCKED, riskAssessmentWithoutScore);

        PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEventMapper.toAvroEvent(paymentResponse);

        assertThat(paymentCompletedEvent.getRiskCheckPassed()).isFalse();
        assertThat(paymentCompletedEvent.getRiskScore()).isNull();
    }

    @Test
    void shouldReportZeroProcessingTimeAndNoScoreWhenThePaymentNeverCompleted() {
        PaymentResponse paymentResponse = new PaymentResponse(
                PAYMENT_UUID.toString(), "PAY-11111111", CUSTOMER_ID.toString(), SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString(), AMOUNT, FROM_CURRENCY, TO_CURRENCY, AMOUNT, null,
                null, null, null, false, PaymentStatus.FAILED, FraudAnalysisStatus.PENDING, null, null,
                INITIATED_AT, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        PaymentResponse paymentResponseWithoutInitiation = new PaymentResponse(
                PAYMENT_UUID.toString(), "PAY-11111111", CUSTOMER_ID.toString(), SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString(), AMOUNT, FROM_CURRENCY, TO_CURRENCY, AMOUNT, null,
                null, null, null, false, PaymentStatus.FAILED, FraudAnalysisStatus.PENDING, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, COMPLETED_AT, null,
                null, null, null, null, null, null, null);

        PaymentCompletedEvent paymentCompletedEvent = PaymentCompletedEventMapper.toAvroEvent(paymentResponse);

        assertThat(paymentCompletedEvent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(paymentCompletedEvent.getRiskScore()).isNull();
        assertThat(paymentCompletedEvent.getProcessingTimeMs()).isZero();
        assertThat(PaymentCompletedEventMapper.toAvroEvent(paymentResponseWithoutInitiation).getProcessingTimeMs()).isZero();
    }

    @ParameterizedTest
    @EnumSource(org.banksolution.enums.PaymentType.class)
    void shouldMapEveryPaymentType(org.banksolution.enums.PaymentType paymentType) {
        PaymentResponse paymentResponse = new PaymentResponse(
                PAYMENT_UUID.toString(), "PAY-11111111", CUSTOMER_ID.toString(), SOURCE_ACCOUNT_ID.toString(),
                DESTINATION_ACCOUNT_ID.toString(), AMOUNT, FROM_CURRENCY, TO_CURRENCY, AMOUNT, null,
                paymentType, null, null, false, PaymentStatus.COMPLETED, FraudAnalysisStatus.APPROVED, null, null,
                INITIATED_AT, null, null, null, null, null, null, null, null, null, null, null, null, COMPLETED_AT, null,
                null, null, null, null, null, null, null);

        assertThat(PaymentCompletedEventMapper.toAvroEvent(paymentResponse).getPaymentType().name()).isEqualTo(paymentType.name());
    }
}
