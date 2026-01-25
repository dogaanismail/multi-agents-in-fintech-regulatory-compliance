package org.banksolution.mapper;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class RiskAssessmentEntityMapper {

    private static final String ML_MODEL_VERSION = "MADDPG-v1.0";

    public static RiskAssessmentEntity toRiskAssessmentEntity(
            FraudAnalysisCompletedEvent event,
            RiskCheckRequestEntity riskCheckRequest) {

        BigDecimal riskScore = BigDecimal.valueOf(event.getConfidence());
        RiskLevel riskLevel = calculateRiskLevel(event.getConfidence());
        RiskAction riskAction = mapToRiskAction(event.getAction().name());
        List<String> fraudIndicators = buildFraudIndicators(event);

        return RiskAssessmentEntity.builder()
                .riskCheckRequest(riskCheckRequest)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .riskAction(riskAction)
                .fraudIndicators(fraudIndicators)
                .mlModelVersion(ML_MODEL_VERSION)
                .processingTimeMs((long) event.getProcessingTimeMs())
                .build();
    }

    private static RiskLevel calculateRiskLevel(double confidence) {
        if (confidence >= 0.8) {
            return RiskLevel.CRITICAL;
        } else if (confidence >= 0.6) {
            return RiskLevel.HIGH;
        } else if (confidence >= 0.4) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    private static RiskAction mapToRiskAction(String action) {
        return switch (action) {
            case "ALLOW" -> RiskAction.PROCEED;
            case "BLOCK" -> RiskAction.BLOCK;
            case "REVIEW" -> RiskAction.ESCALATE;
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
    }

    private static List<String> buildFraudIndicators(FraudAnalysisCompletedEvent event) {
        List<String> indicators = new ArrayList<>();

        if (event.getTransactionAgentObservation().getIsSuspicious()) {
            indicators.add("SUSPICIOUS_TRANSACTION_PATTERN");
        }
        if (event.getCustomerAgentObservation().getIsSuspicious()) {
            indicators.add("SUSPICIOUS_CUSTOMER_BEHAVIOR");
        }
        if (event.getNetworkAgentObservation().getIsSuspicious()) {
            indicators.add("SUSPICIOUS_NETWORK_ACTIVITY");
        }

        return indicators;
    }
}
