package org.banksolution.service;

import com.aml.risk.RiskAssessmentRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.exception.RiskAssessmentProcessingException;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.banksolution.enums.RiskCheckStatus.*;
import static org.banksolution.mapper.RiskCheckRequestEntityMapper.toEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCheckService {

    private final RiskCheckRequestRepository riskCheckRequestRepository;
    private final FraudAnalysisRequestService fraudAnalysisRequestService;

    @Transactional
    public void processRiskAssessmentRequest(RiskAssessmentRequestedEvent event) {
        log.info("Processing risk assessment request for paymentId: {}", event.getPaymentId());

        if (riskCheckRequestRepository.existsByPaymentId(event.getPaymentId())) {
            log.warn("Duplicate risk assessment request received for paymentId: {}, skipping", event.getPaymentId());
            return;
        }

        RiskCheckRequestEntity riskCheckRequestEntity = toEntity(event);
        try {
            RiskCheckRequestEntity savedRiskCheckRequestEntity = riskCheckRequestRepository.save(riskCheckRequestEntity);
            fraudAnalysisRequestService.processFraudAnalysisRequest(savedRiskCheckRequestEntity);
        } catch (Exception e) {
            log.error("Failed to save risk check request for paymentId: {}", event.getPaymentId(), e);
            riskCheckRequestEntity.setStatus(FAILED);
            riskCheckRequestRepository.save(riskCheckRequestEntity);
            throw new RiskAssessmentProcessingException("Failed to save risk check request for paymentId: %s", e, event.getPaymentId());
        }
    }

}
