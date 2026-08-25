package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import org.banksolution.entity.AgentObservationEntity;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.AgentType;
import org.banksolution.repository.AgentObservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createMarlAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentObservationServiceTest {

    @Mock
    private AgentObservationRepository agentObservationRepository;

    @InjectMocks
    private AgentObservationService agentObservationService;

    @Test
    void shouldSaveOneObservationPerAgent() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        MarlAssessmentEntity marlAssessment = createMarlAssessmentEntity(riskCheckRequest);
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), "PAY-1");

        agentObservationService.create(event, marlAssessment);

        ArgumentCaptor<List<AgentObservationEntity>> observationsCaptor = ArgumentCaptor.captor();
        verify(agentObservationRepository).saveAll(observationsCaptor.capture());
        assertThat(observationsCaptor.getValue())
                .extracting(AgentObservationEntity::getAgentType)
                .containsExactly(AgentType.TRANSACTION, AgentType.CUSTOMER, AgentType.NETWORK);
    }
}
