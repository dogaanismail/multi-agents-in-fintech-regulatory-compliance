package org.banksolution.domain.payment.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    private Double riskScore;
    private String riskLevel;
    private String riskAction;
    private List<String> fraudIndicators;
    private String mlModelVersion;
    private Long processingTimeMs;
    private MarlAssessment marlAssessment;

}
