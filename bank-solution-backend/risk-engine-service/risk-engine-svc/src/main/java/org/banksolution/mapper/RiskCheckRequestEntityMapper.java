package org.banksolution.mapper;

import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.PaymentType;

import java.math.BigDecimal;

@UtilityClass
public class RiskCheckRequestEntityMapper {

    public static RiskCheckRequestEntity toEntity(RiskAssessmentRequestedEvent event) {
        return RiskCheckRequestEntity.builder()
                .paymentId(event.getPaymentId())
                .customerId(event.getCustomerId())
                .sourceAccountId(event.getSourceAccountId())
                .destinationAccountId(event.getDestinationAccountId())
                .amount(new BigDecimal(event.getAmount()))
                .currency(event.getCurrency())
                .paymentType(PaymentType.valueOf(event.getPaymentType().name()))
                .description(event.getDescription())
                .requestTimestamp(event.getTimestamp())
                .build();
    }
}
