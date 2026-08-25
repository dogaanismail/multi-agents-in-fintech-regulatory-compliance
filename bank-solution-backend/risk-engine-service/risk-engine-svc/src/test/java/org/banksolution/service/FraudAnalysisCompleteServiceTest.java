package org.banksolution.service;

import com.aml.fraud.FraudAnalysisCompletedEvent;
import org.banksolution.entity.MarlAssessmentEntity;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.RiskCheckStatus;
import org.banksolution.exception.RiskCheckRequestNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.FraudAnalysisFixtures.createFraudAnalysisCompletedEvent;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createMarlAssessmentEntity;
import static org.banksolution.fixtures.RiskAssessmentFixtures.createRiskAssessmentEntity;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudAnalysisCompleteServiceTest {

    private static final String PAYMENT_ID = "PAY-1";

    @Mock
    private RiskCheckRequestService riskCheckRequestService;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @Mock
    private MarlAssessmentService marlAssessmentService;

    @Mock
    private AgentObservationService agentObservationService;

    @Mock
    private RiskAssessmentCompleteService riskAssessmentCompleteService;

    @InjectMocks
    private FraudAnalysisCompleteService fraudAnalysisCompleteService;

    @Test
    void shouldRecordEveryAssessmentAndPublishTheCompletedEvent() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        RiskAssessmentEntity riskAssessment = createRiskAssessmentEntity(riskCheckRequest);
        MarlAssessmentEntity marlAssessment = createMarlAssessmentEntity(riskCheckRequest);
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID);

        when(riskCheckRequestService.findById(riskCheckRequest.getId())).thenReturn(riskCheckRequest);
        when(marlAssessmentService.existsByRiskCheckRequestId(riskCheckRequest.getId())).thenReturn(false);
        when(riskAssessmentService.create(event, riskCheckRequest)).thenReturn(riskAssessment);
        when(marlAssessmentService.create(event, riskCheckRequest)).thenReturn(marlAssessment);

        fraudAnalysisCompleteService.processFraudAnalysisCompleted(event);

        verify(agentObservationService).create(event, marlAssessment);
        verify(riskCheckRequestService).save(riskCheckRequest);
        assertThat(riskCheckRequest.getStatus()).isEqualTo(RiskCheckStatus.COMPLETED);
        verify(riskAssessmentCompleteService)
                .publishRiskAssessmentCompletedEvent(event, riskCheckRequest, riskAssessment);
    }

    @Test
    void shouldSkipADuplicateCompletionWithoutAssessingOrPublishing() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID);

        when(riskCheckRequestService.findById(riskCheckRequest.getId())).thenReturn(riskCheckRequest);
        when(marlAssessmentService.existsByRiskCheckRequestId(riskCheckRequest.getId())).thenReturn(true);

        fraudAnalysisCompleteService.processFraudAnalysisCompleted(event);

        verifyNoInteractions(riskAssessmentService, agentObservationService, riskAssessmentCompleteService);
        assertThat(riskCheckRequest.getStatus()).isNotEqualTo(RiskCheckStatus.COMPLETED);
    }

    @Test
    void shouldPropagateWhenTheRiskCheckRequestIsUnknown() {
        RiskCheckRequestEntity riskCheckRequest = createTransferRiskCheckRequestEntity();
        FraudAnalysisCompletedEvent event =
                createFraudAnalysisCompletedEvent(riskCheckRequest.getId().toString(), PAYMENT_ID);

        when(riskCheckRequestService.findById(riskCheckRequest.getId()))
                .thenThrow(new RiskCheckRequestNotFoundException(riskCheckRequest.getId()));

        assertThatThrownBy(() -> fraudAnalysisCompleteService.processFraudAnalysisCompleted(event))
                .isInstanceOf(RiskCheckRequestNotFoundException.class);

        verifyNoInteractions(riskAssessmentService, agentObservationService, riskAssessmentCompleteService);
    }
}
