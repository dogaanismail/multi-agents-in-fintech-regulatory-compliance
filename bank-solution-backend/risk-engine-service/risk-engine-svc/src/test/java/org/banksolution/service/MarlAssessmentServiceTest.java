package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.repository.MarlAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarlAssessmentServiceTest {

    @Mock
    private MarlAssessmentRepository marlAssessmentRepository;

    @InjectMocks
    private MarlAssessmentService marlAssessmentService;

    @Test
    void shouldMapTheEventAndSaveTheAssessment() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), "PAY-1");
        when(marlAssessmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MarlAssessmentEntity saved = marlAssessmentService.create(event, riskCheckRequest);

        ArgumentCaptor<MarlAssessmentEntity> entityCaptor = ArgumentCaptor.forClass(MarlAssessmentEntity.class);
        verify(marlAssessmentRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue()).isEqualTo(saved);
        assertThat(saved.getRiskCheckRequest()).isEqualTo(riskCheckRequest);
        assertThat(saved.getAction().name()).isEqualTo(event.getAction().name());
    }

    @Test
    void shouldDelegateTheDuplicateCheckToTheRepository() {
        UUID riskCheckRequestId = UUID.randomUUID();
        when(marlAssessmentRepository.existsByRiskCheckRequestId(riskCheckRequestId)).thenReturn(true);

        assertThat(marlAssessmentService.existsByRiskCheckRequestId(riskCheckRequestId)).isTrue();
    }
}
