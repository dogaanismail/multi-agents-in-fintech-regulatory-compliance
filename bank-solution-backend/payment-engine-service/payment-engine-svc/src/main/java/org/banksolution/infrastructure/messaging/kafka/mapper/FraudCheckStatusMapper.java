package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.payment.FraudCheckStatus;
import lombok.experimental.UtilityClass;
import org.banksolution.enums.FraudAnalysisStatus;

/**
 * Mapper for converting domain FraudCheckStatus to Avro FraudCheckStatus.
 */
@UtilityClass
public class FraudCheckStatusMapper {

    public static FraudCheckStatus toAvro(Object fraudStatus) {
        if (fraudStatus == null) {
            return FraudCheckStatus.PENDING;
        }
        
        FraudAnalysisStatus status = (FraudAnalysisStatus) fraudStatus;
        return switch (status) {
            case PENDING -> FraudCheckStatus.PENDING;
            case APPROVED -> FraudCheckStatus.APPROVED;
            case BLOCKED -> FraudCheckStatus.BLOCKED;
            case REVIEW_REQUIRED -> FraudCheckStatus.REVIEW_REQUIRED;
            default -> throw new IllegalArgumentException("Unknown FraudCheckStatus: " + status);
        };
    }
}
