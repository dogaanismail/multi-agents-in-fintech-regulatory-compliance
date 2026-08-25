package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.repository.RiskAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @InjectMocks
    private RiskAssessmentService riskAssessmentService;

    @Test
    void shouldMapTheEventAndSaveTheAssessment() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), "PAY-1");
        when(riskAssessmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RiskAssessmentEntity saved = riskAssessmentService.create(event, riskCheckRequest);

        ArgumentCaptor<RiskAssessmentEntity> entityCaptor = ArgumentCaptor.forClass(RiskAssessmentEntity.class);
        verify(riskAssessmentRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue()).isEqualTo(saved);
        assertThat(saved.getRiskCheckRequest()).isEqualTo(riskCheckRequest);
        assertThat(saved.getMlModelVersion()).isEqualTo("MADDPG-v1.0");
    }
}
