package org.banksolution.fixtures;

import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public final class RiskCheckRequestFixtures {

    public static final String AMOUNT_AS_STRING = "1500.5000";
    public static final BigDecimal AMOUNT = new BigDecimal(AMOUNT_AS_STRING);
    public static final String FROM_CURRENCY = "GBP";
    public static final String TO_CURRENCY = "EUR";
    public static final long REQUEST_TIMESTAMP = 1755000000000L;
    public static final String DESCRIPTION = "Invoice 2026-081";

    private RiskCheckRequestFixtures() {
    }

    public static RiskAssessmentRequestedEvent createRiskAssessmentRequestedEvent(String paymentId) {
        return createRiskAssessmentRequestedEvent(
                paymentId,
                com.aml.risk.PaymentType.TRANSFER_OUT,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    public static RiskAssessmentRequestedEvent createRiskAssessmentRequestedEvent(
            String paymentId,
            com.aml.risk.PaymentType paymentType,
            String sourceAccountId,
            String destinationAccountId) {

        return RiskAssessmentRequestedEvent.newBuilder()
                .setPaymentId(paymentId)
                .setCustomerId(UUID.randomUUID().toString())
                .setPaymentType(paymentType)
                .setTimestamp(REQUEST_TIMESTAMP)
                .setSourceAccountId(sourceAccountId)
                .setDestinationAccountId(destinationAccountId)
                .setAmount(AMOUNT_AS_STRING)
                .setFromCurrency(FROM_CURRENCY)
                .setToCurrency(TO_CURRENCY)
                .setDescription(DESCRIPTION)
                .build();
    }

    public static RiskCheckRequestEntity createTransferRiskCheckRequestEntity() {
        return createRiskCheckRequestEntity(
                PaymentType.TRANSFER_OUT,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString());
    }

    public static RiskCheckRequestEntity createDepositRiskCheckRequestEntity() {
        return createRiskCheckRequestEntity(PaymentType.DEPOSIT, null, UUID.randomUUID().toString());
    }

    public static RiskCheckRequestEntity createWithdrawalRiskCheckRequestEntity() {
        return createRiskCheckRequestEntity(PaymentType.WITHDRAWAL, UUID.randomUUID().toString(), null);
    }

    public static RiskCheckRequestEntity createRiskCheckRequestEntity(
            PaymentType paymentType,
            String sourceAccountId,
            String destinationAccountId) {

        return RiskCheckRequestEntity.builder()
                .id(UUID.randomUUID())
                .paymentId("PAY-" + UUID.randomUUID())
                .customerId(UUID.randomUUID().toString())
                .sourceAccountId(sourceAccountId)
                .destinationAccountId(destinationAccountId)
                .amount(AMOUNT)
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .paymentType(paymentType)
                .description(DESCRIPTION)
                .requestTimestamp(REQUEST_TIMESTAMP)
                .build();
    }
}
