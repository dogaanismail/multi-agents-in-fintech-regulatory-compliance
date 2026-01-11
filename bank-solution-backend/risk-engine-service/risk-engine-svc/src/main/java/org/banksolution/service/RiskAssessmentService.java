package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.repository.RiskAssessmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.banksolution.mapper.RiskAssessmentEntityMapper.toRiskAssessmentEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;

    @Transactional
    public void create(FraudAnalysisCompletedEvent event, RiskCheckRequestEntity riskCheckRequest) {
        RiskAssessmentEntity entity = toRiskAssessmentEntity(event, riskCheckRequest);
        riskAssessmentRepository.save(entity);
    }
}
