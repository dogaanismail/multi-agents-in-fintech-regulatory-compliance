package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.PaymentCompletedEvent;
import com.aml.payment.PaymentType;
import lombok.experimental.UtilityClass;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.enums.FraudAnalysisStatus;

import java.time.Instant;
import java.util.UUID;

@UtilityClass
public class PaymentCompletedEventMapper {

    public static PaymentCompletedEvent toAvroEvent(PaymentResponse paymentResponse) {
        return PaymentCompletedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setPaymentId(paymentResponse.paymentId())
                .setTimestamp(Instant.now().toEpochMilli())
                .setPaymentType(mapPaymentType(paymentResponse.paymentType()))
                .setSourceAccountId(paymentResponse.sourceAccountId())
                .setSourceAccountBankLocation(null)
                .setDestinationAccountId(paymentResponse.destinationAccountId())
                .setDestinationAccountBankLocation(null)
                .setCustomerId(paymentResponse.customerId())
                .setAmount(paymentResponse.amount().toString())
                .setFromCurrency(paymentResponse.fromCurrency())
                .setToCurrency(paymentResponse.toCurrency())
                .setRiskCheckPassed(isRiskCheckPassed(paymentResponse))
                .setRiskScore(getRiskScore(paymentResponse))
                .setProcessingTimeMs(calculateProcessingTimeMs(paymentResponse))
                .build();
    }

    private static PaymentType mapPaymentType(org.banksolution.enums.PaymentType paymentType) {
        if (paymentType == null) {
            return PaymentType.TRANSFER_OUT;
        }

        return switch (paymentType) {
            case TRANSFER_IN -> PaymentType.TRANSFER_IN;
            case TRANSFER_OUT -> PaymentType.TRANSFER_OUT;
            case DEPOSIT -> PaymentType.DEPOSIT;
            case WITHDRAWAL -> PaymentType.WITHDRAWAL;
        };
    }

    private static boolean isRiskCheckPassed(PaymentResponse paymentResponse) {
        return paymentResponse.fraudStatus() == FraudAnalysisStatus.APPROVED;
    }

    private static Double getRiskScore(PaymentResponse paymentResponse) {
        if (paymentResponse.riskAssessment() != null && paymentResponse.riskAssessment().riskScore() != null) {
            return paymentResponse.riskAssessment().riskScore();
        }

        return null;
    }

    private static double calculateProcessingTimeMs(PaymentResponse paymentResponse) {
        if (paymentResponse.initiatedAt() != null && paymentResponse.completedAt() != null) {
            return paymentResponse.completedAt().toEpochMilli() - paymentResponse.initiatedAt().toEpochMilli();
        }

        return 0.0;
    }
}
