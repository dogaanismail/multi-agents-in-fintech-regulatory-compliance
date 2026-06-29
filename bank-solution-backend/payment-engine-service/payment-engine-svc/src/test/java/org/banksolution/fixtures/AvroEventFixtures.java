package org.banksolution.fixtures;

import com.aml.account.AccountChargeCompletedEvent;
import com.aml.payment.PaymentCreatedEvent;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentCompletedEvent;
import com.aml.risk.RiskAssessmentRequestedEvent;
import com.aml.risk.RiskLevel;

import java.util.List;
import java.util.UUID;

public final class AvroEventFixtures {

    public static final long TIMESTAMP = 1_700_000_000_000L;

    private AvroEventFixtures() {
    }

    public static PaymentCreatedEvent paymentCreatedEvent() {
        return PaymentCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setTimestamp(TIMESTAMP)
                .setCustomerId(PaymentFixtures.CUSTOMER_ID.toString())
                .setPaymentType(com.aml.payment.PaymentType.TRANSFER_OUT)
                .setIsCrossBorderPayment(false)
                .setSourceAccountId(PaymentFixtures.SOURCE_ACCOUNT_ID.toString())
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setConvertedAmount(PaymentFixtures.CONVERTED_AMOUNT.toPlainString())
                .setAppliedExchangeRate(PaymentFixtures.EXCHANGE_RATE.toPlainString())
                .setDescription(PaymentFixtures.DESCRIPTION)
                .build();
    }

    public static PaymentCreatedEvent depositPaymentCreatedEvent() {
        return PaymentCreatedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setTimestamp(TIMESTAMP)
                .setCustomerId(PaymentFixtures.CUSTOMER_ID.toString())
                .setPaymentType(com.aml.payment.PaymentType.DEPOSIT)
                .setIsCrossBorderPayment(false)
                .setSourceAccountId(null)
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setConvertedAmount(PaymentFixtures.CONVERTED_AMOUNT.toPlainString())
                .setAppliedExchangeRate(null)
                .setDescription(null)
                .build();
    }

    public static RiskAssessmentCompletedEvent riskAssessmentCompletedEvent(RiskAction action, RiskLevel level, double score) {
        return RiskAssessmentCompletedEvent.newBuilder()
                .setRiskCheckRequestId(UUID.randomUUID().toString())
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setRiskScore(score)
                .setRiskLevel(level)
                .setAction(action)
                .setFraudIndicators(List.of("NONE"))
                .setMlModelVersion("model-v1")
                .setProcessingTimeMs(12L)
                .setTimestamp(TIMESTAMP)
                .build();
    }

    public static AccountChargeCompletedEvent accountChargeCompletedEvent(boolean success, String failureReason) {
        return AccountChargeCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setTimestamp(TIMESTAMP)
                .setSourceAccountId(PaymentFixtures.SOURCE_ACCOUNT_ID.toString())
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setPaymentType(com.aml.account.PaymentType.TRANSFER_OUT)
                .setSuccess(success)
                .setFailureReason(failureReason)
                .build();
    }

    public static RiskAssessmentRequestedEvent riskAssessmentRequestedEvent() {
        return RiskAssessmentRequestedEvent.newBuilder()
                .setPaymentId(PaymentFixtures.PAYMENT_UUID.toString())
                .setCustomerId(PaymentFixtures.CUSTOMER_ID.toString())
                .setPaymentType(com.aml.risk.PaymentType.TRANSFER_OUT)
                .setTimestamp(TIMESTAMP)
                .setSourceAccountId(PaymentFixtures.SOURCE_ACCOUNT_ID.toString())
                .setDestinationAccountId(PaymentFixtures.DESTINATION_ACCOUNT_ID.toString())
                .setAmount(PaymentFixtures.AMOUNT.toPlainString())
                .setFromCurrency(PaymentFixtures.FROM_CURRENCY)
                .setToCurrency(PaymentFixtures.TO_CURRENCY)
                .setDescription(PaymentFixtures.DESCRIPTION)
                .build();
    }
}
