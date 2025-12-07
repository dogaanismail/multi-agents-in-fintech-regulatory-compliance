package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.PaymentStatus;
import lombok.experimental.UtilityClass;

/**
 * Mapper for converting domain PaymentStatus to Avro PaymentStatus.
 */
@UtilityClass
public class PaymentStatusMapper {

    public static PaymentStatus toAvro(Object status) {
        if (status == null) {
            return PaymentStatus.INITIATED;
        }
        
        org.banksolution.enums.PaymentStatus paymentStatus = (org.banksolution.enums.PaymentStatus) status;
        return switch (paymentStatus) {
            case INITIATED -> PaymentStatus.INITIATED;
            case FRAUD_CHECK_PENDING -> PaymentStatus.FRAUD_CHECK_PENDING;
            case MANUAL_REVIEW_REQUIRED -> PaymentStatus.MANUAL_REVIEW_REQUIRED;
            case COMPLETED -> PaymentStatus.COMPLETED;
            case BLOCKED -> PaymentStatus.BLOCKED;
            default -> throw new IllegalArgumentException("Unknown PaymentStatus: " + paymentStatus);
        };
    }
}
