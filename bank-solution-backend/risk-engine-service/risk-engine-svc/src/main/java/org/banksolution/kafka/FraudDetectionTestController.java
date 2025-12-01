package org.banksolution.kafka;

import com.aml.fraud.*;
import com.aml.customer.CustomerFeatures;
import com.aml.network.NetworkFeatures;
import com.aml.transaction.TransactionFeatures;
import com.aml.agents.AgentObservation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Test controller to verify Avro schema library integration
 */
@Slf4j
@RestController
@RequestMapping("/api/test/avro")
public class FraudDetectionTestController {

    @GetMapping("/create-request")
    public FraudDetectionRequest createTestRequest() {
        log.info("Creating test FraudDetectionRequest from Avro schema");

        // Build Transaction Features
        TransactionFeatures transactionFeatures = TransactionFeatures.newBuilder()
                .setDate("2025-12-01")
                .setTime("14:30:00")
                .setFromBank("Chase Bank")
                .setFromAccount("ACC-12345")
                .setToBank("HSBC Bank")
                .setToAccount("ACC-67890")
                .setAmountReceived(1500.50)
                .setReceivingCurrency("USD")
                .setAmountPaid(1500.50)
                .setPaymentCurrency("USD")
                .setPaymentType("WIRE_TRANSFER")
                .setSenderBankLocation("US")
                .setReceiverBankLocation("UK")
                .build();

        // Build Customer Features
        CustomerFeatures customerFeatures = CustomerFeatures.newBuilder()
                .setTransactionCount(150)
                .setTotalAmount(75000.0)
                .setAvgAmount(500.0)
                .setMedianAmount(450.0)
                .setMaxAmount(5000.0)
                .setMinAmount(50.0)
                .setStdAmount(200.0)
                .setActiveDays(120)
                .setTransactionsPerDay(1.25)
                .setCrossBorderRatio(0.15)
                .setCashTransactionRatio(0.05)
                .setAmountConsistency(0.85)
                .setLargeTransactionRatio(0.08)
                .setUniqueReceivers(45)
                .setUniqueReceiverCountries(5)
                .setReceiverDiversity(0.75)
                .setNightTransactionRatio(0.02)
                .setWeekendTransactionRatio(0.12)
                .setUniqueCurrencies(3)
                .build();

        // Build Network Features
        NetworkFeatures networkFeatures = NetworkFeatures.newBuilder()
                .setInDegree(10)
                .setOutDegree(15)
                .setDegreeCentrality(0.025)
                .setInDegreeCentrality(0.020)
                .setOutDegreeCentrality(0.030)
                .setBetweennessCentrality(0.15)
                .setClosenessCentrality(0.45)
                .setPagerank(0.025)
                .setEigenvectorCentrality(0.20)
                .setClusteringCoefficient(0.35)
                .setCommunity(1)
                .build();

        // Build Fraud Detection Request
        FraudDetectionRequest request = FraudDetectionRequest.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8))
                .setTimestamp(Instant.now().toEpochMilli())
                .setTransactionFeatures(transactionFeatures)
                .setCustomerFeatures(customerFeatures)
                .setNetworkFeatures(networkFeatures)
                .build();

        log.info("✅ Successfully created FraudDetectionRequest: {}", request.getTransactionId());
        log.info("   Transaction Amount: ${}", request.getTransactionFeatures().getAmountReceived());
        log.info("   Customer Transactions: {}", request.getCustomerFeatures().getTransactionCount());
        log.info("   Network PageRank: {}", request.getNetworkFeatures().getPagerank());

        return request;
    }

    @GetMapping("/create-response")
    public FraudDetectionResponse createTestResponse() {
        log.info("Creating test FraudDetectionResponse from Avro schema");

        // Build Agent Observations
        AgentObservation transactionAgentObs = AgentObservation.newBuilder()
                .setAgentName("transaction-pattern-agent")
                .setIsSuspicious(true)
                .setProbability(0.92)
                .setRiskScore(85.0)
                .setConfidence("HIGH")
                .setResponseTimeMs(45.0)
                .build();

        AgentObservation customerAgentObs = AgentObservation.newBuilder()
                .setAgentName("customer-risk-agent")
                .setIsSuspicious(false)
                .setProbability(0.15)
                .setRiskScore(20.0)
                .setConfidence("HIGH")
                .setResponseTimeMs(30.0)
                .build();

        AgentObservation networkAgentObs = AgentObservation.newBuilder()
                .setAgentName("network-analysis-agent")
                .setIsSuspicious(false)
                .setProbability(0.05)
                .setRiskScore(10.0)
                .setConfidence("MEDIUM")
                .setResponseTimeMs(50.0)
                .build();

        // Build Response
        FraudDetectionResponse response = FraudDetectionResponse.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setTransactionId("TXN-TEST-001")
                .setAction(FraudAction.ALLOW)
                .setConfidence(0.85)
                .setMaddpgQValue(0.75)
                .setTimestamp(Instant.now().toEpochMilli())
                .setTransactionAgentObservation(transactionAgentObs)
                .setCustomerAgentObservation(customerAgentObs)
                .setNetworkAgentObservation(networkAgentObs)
                .setAgentContributions(java.util.Map.of(
                        "transaction", 0.50,
                        "customer", 0.30,
                        "network", 0.20
                ))
                .setProcessingTimeMs(125.0)
                .setMode("inference")
                .build();

        log.info("✅ Successfully created FraudDetectionResponse: {}", response.getTransactionId());
        log.info("   Action: {}", response.getAction());
        log.info("   Confidence: {}", response.getConfidence());
        log.info("   MADDPG Q-Value: {}", response.getMaddpgQValue());

        return response;
    }

    @GetMapping("/verify-classes")
    public String verifyAvroClasses() {
        log.info("Verifying Avro schema classes are accessible...");

        StringBuilder result = new StringBuilder();
        result.append("✅ Avro Schema Library Integration Verified!\n\n");
        result.append("Successfully imported classes:\n");
        result.append("  - FraudDetectionRequest (com.aml.fraud)\n");
        result.append("  - FraudDetectionResponse (com.aml.fraud)\n");
        result.append("  - TransactionFeatures (com.aml.fraud)\n");
        result.append("  - CustomerFeatures (com.aml.fraud)\n");
        result.append("  - NetworkFeatures (com.aml.fraud)\n");
        result.append("  - AgentObservation (com.aml.fraud)\n");
        result.append("  - FraudAction (com.aml.fraud)\n\n");
        result.append("Test endpoints:\n");
        result.append("  GET /api/test/avro/create-request\n");
        result.append("  GET /api/test/avro/create-response\n");
        result.append("  GET /api/test/avro/verify-classes\n");

        log.info(result.toString());
        return result.toString();
    }
}
