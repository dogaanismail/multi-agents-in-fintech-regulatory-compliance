package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AgentObservationEntity;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.repository.AgentObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.banksolution.mapper.MarlAssessmentMapper.toAgentObservations;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentObservationService {

    private final AgentObservationRepository agentObservationRepository;

    @Transactional
    public void create(FraudAnalysisCompletedEvent event, MarlAssessmentEntity marlAssessment) {
        List<AgentObservationEntity> observations = toAgentObservations(event, marlAssessment);
        agentObservationRepository.saveAll(observations);
    }
}
