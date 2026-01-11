package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.banksolution.enums.RiskCheckStatus.COMPLETED;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisCompleteService {

    private final RiskCheckRequestService riskCheckRequestService;
    private final RiskAssessmentService riskAssessmentService;
    private final MarlAssessmentService marlAssessmentService;
    private final AgentObservationService agentObservationService;
    private final RiskAssessmentCompleteService riskAssessmentCompleteService;

    @Transactional
    public void processFraudAnalysisCompleted(FraudAnalysisCompletedEvent event) {
        log.info("Processing fraud analysis completed for paymentId: {}, riskCheckRequestId: {}, action: {}",
                event.getPaymentId(),
                event.getRiskCheckRequestId(),
                event.getAction());

        UUID riskCheckRequestId = UUID.fromString(event.getRiskCheckRequestId());
        RiskCheckRequestEntity riskCheckRequestEntity = riskCheckRequestService.findById(riskCheckRequestId);

        if (marlAssessmentService.existsByRiskCheckRequestId(riskCheckRequestId)) {
            log.warn("Duplicate fraud analysis completed event for riskCheckRequestId: {}, skipping", riskCheckRequestId);
            return;
        }

        RiskAssessmentEntity riskAssessmentEntity = riskAssessmentService.create(event, riskCheckRequestEntity);
        MarlAssessmentEntity marlAssessment = marlAssessmentService.create(event, riskCheckRequestEntity);
        agentObservationService.create(event, marlAssessment);

        riskCheckRequestEntity.setStatus(COMPLETED);
        riskCheckRequestService.save(riskCheckRequestEntity);

        //TODO: Investigate and implement outbox pattern here
        riskAssessmentCompleteService.publishRiskAssessmentCompletedEvent(event, riskCheckRequestEntity, riskAssessmentEntity);

        log.info("Successfully processed fraud analysis for paymentId: {}, riskCheckRequestId: {} and action: {}",
                event.getPaymentId(),
                riskCheckRequestId,
                event.getAction());
    }

}
