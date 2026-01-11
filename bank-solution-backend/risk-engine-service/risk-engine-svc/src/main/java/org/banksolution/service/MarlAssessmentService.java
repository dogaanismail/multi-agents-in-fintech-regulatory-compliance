package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.repository.MarlAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.banksolution.mapper.MarlAssessmentMapper.toMarlAssessmentEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarlAssessmentService {

    private final MarlAssessmentRepository marlAssessmentRepository;

    @Transactional
    public MarlAssessmentEntity create(FraudAnalysisCompletedEvent event, RiskCheckRequestEntity riskCheckRequest) {
        MarlAssessmentEntity entity = toMarlAssessmentEntity(event, riskCheckRequest);
        return marlAssessmentRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public boolean existsByRiskCheckRequestId(UUID riskCheckRequestId) {
        return marlAssessmentRepository.existsByRiskCheckRequestId(riskCheckRequestId);
    }
}