package org.banksolution.fixtures;

import com.aml.risk.RiskAction;
import com.aml.risk.RiskLevel;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class RiskAssessmentFixtures {

    private RiskAssessmentFixtures() {
    }

    public static RiskAssessmentEntity createRiskAssessmentEntity(RiskCheckRequestEntity riskCheckRequest) {
        return RiskAssessmentEntity.builder()
                .id(UUID.randomUUID())
                .riskCheckRequest(riskCheckRequest)
                .riskScore(new BigDecimal("0.6500"))
                .riskLevel(RiskLevel.HIGH)
                .riskAction(RiskAction.ESCALATE)
                .fraudIndicators(List.of("SUSPICIOUS_TRANSACTION_PATTERN"))
                .mlModelVersion("MADDPG-v1.0")
                .processingTimeMs(123L)
                .build();
    }

    public static MarlAssessmentEntity createMarlAssessmentEntity(RiskCheckRequestEntity riskCheckRequest) {
        return MarlAssessmentEntity.builder()
                .id(UUID.randomUUID())
                .riskCheckRequest(riskCheckRequest)
                .action(com.aml.risk.MarlAction.REVIEW)
                .confidence(new BigDecimal("0.6500"))
                .maddpgQValue(new BigDecimal("0.123400"))
                .processingTimeMs(new BigDecimal("123.45"))
                .mode(FraudAnalysisFixtures.MODE)
                .responseTimestamp(FraudAnalysisFixtures.RESPONSE_TIMESTAMP)
                .build();
    }
}
