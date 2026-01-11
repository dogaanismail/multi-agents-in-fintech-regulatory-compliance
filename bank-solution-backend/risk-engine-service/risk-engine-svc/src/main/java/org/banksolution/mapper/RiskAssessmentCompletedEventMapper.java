package org.banksolution.mapper;

import com.aml.fraud.CustomerAgentObservation;
import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.fraud.NetworkAgentObservation;
import com.aml.fraud.TransactionAgentObservation;
import com.aml.risk.MarlAssessment;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentCompletedEvent;
import com.aml.risk.RiskLevel;
import lombok.experimental.UtilityClass;
import org.banksolution.entity.RiskAssessmentEntity;
import org.banksolution.entity.RiskCheckRequestEntity;

import java.time.Instant;
import java.util.ArrayList;

@UtilityClass
public class RiskAssessmentCompletedEventMapper {

    public static RiskAssessmentCompletedEvent toEvent(
            FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent,
            RiskCheckRequestEntity riskCheckRequest,
            RiskAssessmentEntity riskAssessment,
            long processingTimeMs) {

        return RiskAssessmentCompletedEvent.newBuilder()
                .setRiskCheckRequestId(riskCheckRequest.getId().toString())
                .setPaymentId(riskCheckRequest.getPaymentId())
                .setRiskScore(riskAssessment.getRiskScore().doubleValue())
                .setRiskLevel(RiskLevel.valueOf(riskAssessment.getRiskLevel().name()))
                .setAction(RiskAction.valueOf(riskAssessment.getRiskAction().name()))
                .setFraudIndicators(new ArrayList<>(riskAssessment.getFraudIndicators()))
                .setMlModelVersion(riskAssessment.getMlModelVersion())
                .setMarlAssessment(toMarlAssessment(fraudAnalysisCompletedEvent, riskCheckRequest))
                .setProcessingTimeMs(processingTimeMs)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    private static MarlAssessment toMarlAssessment(
            FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent,
            RiskCheckRequestEntity riskCheckRequest) {

        return MarlAssessment.newBuilder()
                .setRequestId(riskCheckRequest.getId().toString())
                .setAction(com.aml.risk.MarlAction.valueOf(fraudAnalysisCompletedEvent.getAction().name()))
                .setConfidence(fraudAnalysisCompletedEvent.getConfidence())
                .setMaddpgQValue(fraudAnalysisCompletedEvent.getMaddpgQValue())
                .setTransactionAgentObservation(toTransactionAgentObservation(fraudAnalysisCompletedEvent))
                .setCustomerAgentObservation(toCustomerAgentObservation(fraudAnalysisCompletedEvent))
                .setNetworkAgentObservation(toNetworkAgentObservation(fraudAnalysisCompletedEvent))
                .setAgentContributions(fraudAnalysisCompletedEvent.getAgentContributions())
                .setProcessingTimeMs(fraudAnalysisCompletedEvent.getProcessingTimeMs())
                .setMode(fraudAnalysisCompletedEvent.getMode())
                .build();
    }

    private static com.aml.risk.TransactionAgentObservation toTransactionAgentObservation(
            FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent) {

        TransactionAgentObservation transactionAgentObservation = fraudAnalysisCompletedEvent.getTransactionAgentObservation();
        return com.aml.risk.TransactionAgentObservation.newBuilder()
                .setAgentName(transactionAgentObservation.getAgentName())
                .setIsSuspicious(transactionAgentObservation.getIsSuspicious())
                .setProbability(transactionAgentObservation.getProbability())
                .setRiskScore(transactionAgentObservation.getRiskScore())
                .setConfidence(transactionAgentObservation.getConfidence())
                .setResponseTimeMs(transactionAgentObservation.getResponseTimeMs())
                .build();
    }

    private static com.aml.risk.CustomerAgentObservation toCustomerAgentObservation(
            FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent) {

        CustomerAgentObservation customerAgentObservation = fraudAnalysisCompletedEvent.getCustomerAgentObservation();
        return com.aml.risk.CustomerAgentObservation.newBuilder()
                .setAgentName(customerAgentObservation.getAgentName())
                .setIsSuspicious(customerAgentObservation.getIsSuspicious())
                .setProbability(customerAgentObservation.getProbability())
                .setRiskScore(customerAgentObservation.getRiskScore())
                .setConfidence(customerAgentObservation.getConfidence())
                .setResponseTimeMs(customerAgentObservation.getResponseTimeMs())
                .build();
    }

    private static com.aml.risk.NetworkAgentObservation toNetworkAgentObservation(
            FraudAnalysisCompletedEvent fraudAnalysisCompletedEvent) {

        NetworkAgentObservation networkAgentObservation = fraudAnalysisCompletedEvent.getNetworkAgentObservation();
        return com.aml.risk.NetworkAgentObservation.newBuilder()
                .setAgentName(networkAgentObservation.getAgentName())
                .setIsSuspicious(networkAgentObservation.getIsSuspicious())
                .setProbability(networkAgentObservation.getProbability())
                .setRiskScore(networkAgentObservation.getRiskScore())
                .setConfidence(networkAgentObservation.getConfidence())
                .setResponseTimeMs(networkAgentObservation.getResponseTimeMs())
                .build();
    }
}
