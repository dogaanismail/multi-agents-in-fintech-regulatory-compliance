package org.banksolution.domain.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.domain.payment.valueobject.RiskAssessment;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckCompletedEvent {
    
    private PaymentId paymentId;
    private RiskAssessment riskAssessment;
}
