package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.risk.RiskAssessmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.infrastructure.messaging.kafka.producer.RiskAssessmentCompletedEventProducer;
import org.springframework.stereotype.Service;

import static org.banksolution.mapper.RiskAssessmentCompletedEventMapper.toEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentCompleteService {

    private final RiskAssessmentCompletedEventProducer riskAssessmentCompletedEventProducer;

    public void publishRiskAssessmentCompletedEvent(
            FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent,
            RiskCheckRequestEntity riskCheckRequestEntity,
            RiskAssessmentEntity riskAssessmentEntity) {

        RiskAssessmentCompletedEvent riskAssessmentCompletedEvent = toEvent(
                fraudAnalysisCompletedEvent,
                riskCheckRequestEntity,
                riskAssessmentEntity,
                fraudAnalysisCompletedEvent.getTimestamp()
        );

        riskAssessmentCompletedEventProducer.produceRiskAssessmentCompletedEvent(riskAssessmentCompletedEvent);
    }
}
