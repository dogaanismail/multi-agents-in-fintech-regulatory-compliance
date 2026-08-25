package org.banksolution.service;

import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.RiskCheckStatus;
import org.banksolution.exception.RiskAssessmentProcessingException;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createRiskAssessmentRequestedEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentRequestServiceTest {

    private static final String PAYMENT_ID = "PAY-1";

    @Mock
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @Mock
    private FraudAnalysisRequestService fraudAnalysisRequestService;

    @InjectMocks
    private RiskAssessmentRequestService riskAssessmentRequestService;

    @Test
    void shouldPersistTheRequestAndTriggerFraudAnalysis() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);
        RiskCheckRequestEntity savedEntity = RiskCheckRequestEntity.builder().id(UUID.randomUUID()).build();
        when(riskCheckRequestRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false);
        when(riskCheckRequestRepository.save(any())).thenReturn(savedEntity);

        riskAssessmentRequestService.processRiskAssessmentRequest(event);

        ArgumentCaptor<RiskCheckRequestEntity> entityCaptor = ArgumentCaptor.forClass(RiskCheckRequestEntity.class);
        verify(riskCheckRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getPaymentId()).isEqualTo(PAYMENT_ID);
        verify(fraudAnalysisRequestService).processFraudAnalysisRequest(savedEntity);
    }

    @Test
    void shouldSkipADuplicatePaymentWithoutSavingOrAnalysing() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);
        when(riskCheckRequestRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(true);

        riskAssessmentRequestService.processRiskAssessmentRequest(event);

        verify(riskCheckRequestRepository, never()).save(any());
        verifyNoInteractions(fraudAnalysisRequestService);
    }

    @Test
    void shouldMarkTheRequestFailedAndRethrowWhenSavingFails() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);
        when(riskCheckRequestRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false);
        when(riskCheckRequestRepository.save(any()))
                .thenThrow(new IllegalStateException("database down"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> riskAssessmentRequestService.processRiskAssessmentRequest(event))
                .isInstanceOf(RiskAssessmentProcessingException.class)
                .hasMessageContaining(PAYMENT_ID);

        ArgumentCaptor<RiskCheckRequestEntity> entityCaptor = ArgumentCaptor.forClass(RiskCheckRequestEntity.class);
        verify(riskCheckRequestRepository, times(2)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(RiskCheckStatus.FAILED);
        verifyNoInteractions(fraudAnalysisRequestService);
    }

    @Test
    void shouldMarkTheRequestFailedAndRethrowWhenFraudAnalysisFails() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);
        RiskCheckRequestEntity savedEntity = RiskCheckRequestEntity.builder().id(UUID.randomUUID()).build();
        when(riskCheckRequestRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false);
        when(riskCheckRequestRepository.save(any())).thenReturn(savedEntity);
        doThrow(new IllegalStateException("orchestrator down"))
                .when(fraudAnalysisRequestService).processFraudAnalysisRequest(savedEntity);

        assertThatThrownBy(() -> riskAssessmentRequestService.processRiskAssessmentRequest(event))
                .isInstanceOf(RiskAssessmentProcessingException.class)
                .hasMessageContaining(PAYMENT_ID);

        ArgumentCaptor<RiskCheckRequestEntity> entityCaptor = ArgumentCaptor.forClass(RiskCheckRequestEntity.class);
        verify(riskCheckRequestRepository, times(2)).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getStatus()).isEqualTo(RiskCheckStatus.FAILED);
    }
}
