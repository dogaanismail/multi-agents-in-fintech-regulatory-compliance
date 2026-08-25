package org.banksolution.fixtures;

import com.aml.fraud.CustomerAgentObservation;
import com.aml.fraud.FeatureContribution;
import com.aml.fraud.FraudAction;
import com.aml.fraud.FraudAnalysisCompletedEvent;
import com.aml.fraud.NetworkAgentObservation;
import com.aml.fraud.TransactionAgentObservation;

import java.util.List;
import java.util.Map;

public final class FraudAnalysisFixtures {

    public static final double CONFIDENCE = 0.65;
    public static final double MADDPG_Q_VALUE = 0.1234;
    public static final double PROCESSING_TIME_MS = 123.45;
    public static final long RESPONSE_TIMESTAMP = 1755000100000L;
    public static final String MODE = "inference";
    public static final Map<String, Double> AGENT_CONTRIBUTIONS =
            Map.of("transaction", 0.5, "customer", 0.3, "network", 0.2);

    private FraudAnalysisFixtures() {
    }

    public static FraudAnalysisCompletedEvent createFraudAnalysisCompletedEvent(
            String riskCheckRequestId,
            String paymentId) {

        return createFraudAnalysisCompletedEvent(riskCheckRequestId, paymentId, FraudAction.REVIEW, CONFIDENCE);
    }

    public static FraudAnalysisCompletedEvent createFraudAnalysisCompletedEvent(
            String riskCheckRequestId,
            String paymentId,
            FraudAction action,
            double confidence) {

        return FraudAnalysisCompletedEvent.newBuilder()
                .setRiskCheckRequestId(riskCheckRequestId)
                .setPaymentId(paymentId)
                .setAction(action)
                .setConfidence(confidence)
                .setMaddpgQValue(MADDPG_Q_VALUE)
                .setTransactionAgentObservation(createTransactionAgentObservation(true))
                .setCustomerAgentObservation(createCustomerAgentObservation(false))
                .setNetworkAgentObservation(createNetworkAgentObservation(false))
                .setAgentContributions(AGENT_CONTRIBUTIONS)
                .setProcessingTimeMs(PROCESSING_TIME_MS)
                .setTimestamp(RESPONSE_TIMESTAMP)
                .setMode(MODE)
                .build();
    }

    public static TransactionAgentObservation createTransactionAgentObservation(boolean isSuspicious) {
        return TransactionAgentObservation.newBuilder()
                .setAgentName("transaction-pattern-agent")
                .setIsSuspicious(isSuspicious)
                .setProbability(0.91)
                .setRiskScore(88.5)
                .setConfidence("HIGH")
                .setResponseTimeMs(45.12)
                .setFeatureContributions(createFeatureContributions())
                .setShapBaseValue(-1.25)
                .build();
    }

    public static CustomerAgentObservation createCustomerAgentObservation(boolean isSuspicious) {
        return CustomerAgentObservation.newBuilder()
                .setAgentName("customer-risk-agent")
                .setIsSuspicious(isSuspicious)
                .setProbability(0.12)
                .setRiskScore(23.0)
                .setConfidence("MEDIUM")
                .setResponseTimeMs(38.9)
                .setFeatureContributions(null)
                .setShapBaseValue(null)
                .build();
    }

    public static NetworkAgentObservation createNetworkAgentObservation(boolean isSuspicious) {
        return NetworkAgentObservation.newBuilder()
                .setAgentName("network-analysis-agent")
                .setIsSuspicious(isSuspicious)
                .setProbability(0.33)
                .setRiskScore(41.7)
                .setConfidence("LOW")
                .setResponseTimeMs(52.4)
                .setFeatureContributions(createFeatureContributions())
                .setShapBaseValue(0.42)
                .build();
    }

    public static List<FeatureContribution> createFeatureContributions() {
        return List.of(
                FeatureContribution.newBuilder()
                        .setFeature("amount")
                        .setValue("1500.50")
                        .setShapValue(0.87)
                        .setDirection("INCREASES_RISK")
                        .build(),
                FeatureContribution.newBuilder()
                        .setFeature("cross_border_ratio")
                        .setValue("0.05")
                        .setShapValue(-0.14)
                        .setDirection("DECREASES_RISK")
                        .build());
    }
}
