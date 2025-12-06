package org.banksolution.service;

import com.aml.payment.PaymentSnapshotEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.PaymentHistory;
import org.banksolution.repository.PaymentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Simplified service that stores payment snapshots directly.
 * No complex aggregation logic needed - just save the snapshot as-is.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryAggregationService {

    private final PaymentHistoryRepository repository;

    /**
     * Process payment snapshot event - create or update payment history record.
     * Uses upsert logic: create if not exists, update if exists.
     */
    @Transactional
    public void processPaymentSnapshot(PaymentSnapshotEvent snapshot) {
        log.info("Processing payment snapshot: referenceNumber={}, version={}, trigger={}",
                snapshot.getReferenceNumber(), snapshot.getVersion(), snapshot.getEventTrigger());

        Optional<PaymentHistory> existingHistory = 
                repository.findByReferenceNumber(snapshot.getReferenceNumber().toString());

        PaymentHistory history;
        if (existingHistory.isPresent()) {
            // Update existing record
            history = existingHistory.get();
            log.info("Updating existing payment history: referenceNumber={}, old version={}, new version={}",
                    snapshot.getReferenceNumber(), history.getVersion(), snapshot.getVersion());
        } else {
            // Create new record
            history = new PaymentHistory();
            history.setPaymentId(UUID.fromString(snapshot.getPaymentId().toString()));
            log.info("Creating new payment history: referenceNumber={}", snapshot.getReferenceNumber());
        }

        // Map snapshot to history entity
        mapSnapshotToHistory(snapshot, history);

        repository.save(history);
        log.info("Payment history saved: referenceNumber={}, version={}", 
                snapshot.getReferenceNumber(), history.getVersion());
    }

    /**
     * Map Avro PaymentSnapshotEvent to PaymentHistory entity
     */
    private void mapSnapshotToHistory(PaymentSnapshotEvent snapshot, PaymentHistory history) {
        // Basic payment information
        history.setExternalPaymentId(UUID.fromString(snapshot.getExternalPaymentId().toString()));
        history.setReferenceNumber(snapshot.getReferenceNumber().toString());
        history.setCustomerId(UUID.fromString(snapshot.getCustomerId().toString()));
        history.setSourceAccountId(UUID.fromString(snapshot.getSourceAccountId().toString()));
        history.setDestinationAccountId(UUID.fromString(snapshot.getDestinationAccountId().toString()));
        history.setAmount(new java.math.BigDecimal(snapshot.getAmount().toString()));
        history.setCurrency(snapshot.getCurrency().toString());
        history.setPaymentType(snapshot.getPaymentType().toString());
        history.setDescription(snapshot.getDescription() != null ? snapshot.getDescription().toString() : null);

        // Status
        history.setStatus(snapshot.getStatus().toString());
        history.setFraudStatus(snapshot.getFraudStatus().toString());

        // Risk Assessment
        if (snapshot.getRiskAssessment() != null) {
            var riskAssessment = snapshot.getRiskAssessment();
            history.setRiskScore(riskAssessment.getRiskScore());
            history.setRiskLevel(riskAssessment.getRiskLevel().toString());
            history.setRiskAction(riskAssessment.getRiskAction().toString());
            history.setFraudIndicators(riskAssessment.getFraudIndicators() != null ? 
                    new ArrayList<>(riskAssessment.getFraudIndicators()) : new ArrayList<>());
            history.setMlModelVersion(riskAssessment.getMlModelVersion() != null ? 
                    riskAssessment.getMlModelVersion().toString() : null);
            history.setRiskProcessingTimeMs(riskAssessment.getProcessingTimeMs());

            // MARL Assessment
            if (riskAssessment.getMarlAssessment() != null) {
                PaymentHistory.MarlAssessment marlAssessment = new PaymentHistory.MarlAssessment();
                var sourceMarlAssessment = riskAssessment.getMarlAssessment();

                marlAssessment.setRequestId(sourceMarlAssessment.getRequestId().toString());
                marlAssessment.setAction(sourceMarlAssessment.getAction().toString());
                marlAssessment.setConfidence(sourceMarlAssessment.getConfidence());
                marlAssessment.setMaddpgQValue(sourceMarlAssessment.getMaddpgQValue());
                marlAssessment.setProcessingTimeMs(sourceMarlAssessment.getProcessingTimeMs());
                marlAssessment.setMode(sourceMarlAssessment.getMode().toString());

                // Map agent observations
                if (sourceMarlAssessment.getTransactionAgentObservation() != null) {
                    marlAssessment.setTransactionAgentObservation(
                            mapTransactionAgentObservation(sourceMarlAssessment.getTransactionAgentObservation()));
                }
                if (sourceMarlAssessment.getCustomerAgentObservation() != null) {
                    marlAssessment.setCustomerAgentObservation(
                            mapCustomerAgentObservation(sourceMarlAssessment.getCustomerAgentObservation()));
                }
                if (sourceMarlAssessment.getNetworkAgentObservation() != null) {
                    marlAssessment.setNetworkAgentObservation(
                            mapNetworkAgentObservation(sourceMarlAssessment.getNetworkAgentObservation()));
                }

                // Map agent contributions
                if (sourceMarlAssessment.getAgentContributions() != null) {
                    marlAssessment.setAgentContributions(new HashMap<>(sourceMarlAssessment.getAgentContributions()));
                }

                history.setMarlAssessment(marlAssessment);
                history.setMarlProcessingTimeMs(sourceMarlAssessment.getProcessingTimeMs());
            }
        }

        // Lifecycle Timestamps - convert from Long (epoch millis) to Instant
        history.setInitiatedAt(snapshot.getInitiatedAt() != null ? 
                Instant.ofEpochMilli(snapshot.getInitiatedAt()) : null);
        history.setRiskCheckRequestedAt(snapshot.getRiskCheckRequestedAt() != null ? 
                Instant.ofEpochMilli(snapshot.getRiskCheckRequestedAt()) : null);
        history.setRiskCheckCompletedAt(snapshot.getRiskCheckCompletedAt() != null ? 
                Instant.ofEpochMilli(snapshot.getRiskCheckCompletedAt()) : null);
        history.setCompletedAt(snapshot.getCompletedAt() != null ? 
                Instant.ofEpochMilli(snapshot.getCompletedAt()) : null);
        history.setBlockedAt(snapshot.getBlockedAt() != null ? 
                Instant.ofEpochMilli(snapshot.getBlockedAt()) : null);
        history.setManualReviewRequestedAt(snapshot.getManualReviewRequestedAt() != null ? 
                Instant.ofEpochMilli(snapshot.getManualReviewRequestedAt()) : null);

        // Snapshot Metadata
        history.setVersion(snapshot.getVersion());
    }

    /**
     * Map transaction agent observation from Avro to entity
     */
    private PaymentHistory.AgentObservation mapTransactionAgentObservation(
            com.aml.payment.TransactionAgentObservationSnapshot source) {
        PaymentHistory.AgentObservation observation = new PaymentHistory.AgentObservation();
        observation.setAgentName(source.getAgentName().toString());
        observation.setIsSuspicious(source.getIsSuspicious());
        observation.setProbability(source.getProbability());
        observation.setRiskScore(source.getRiskScore());
        observation.setConfidence(source.getConfidence().toString());
        observation.setResponseTimeMs(source.getResponseTimeMs());
        return observation;
    }

    /**
     * Map customer agent observation from Avro to entity
     */
    private PaymentHistory.AgentObservation mapCustomerAgentObservation(
            com.aml.payment.CustomerAgentObservationSnapshot source) {
        PaymentHistory.AgentObservation observation = new PaymentHistory.AgentObservation();
        observation.setAgentName(source.getAgentName().toString());
        observation.setIsSuspicious(source.getIsSuspicious());
        observation.setProbability(source.getProbability());
        observation.setRiskScore(source.getRiskScore());
        observation.setConfidence(source.getConfidence().toString());
        observation.setResponseTimeMs(source.getResponseTimeMs());
        return observation;
    }

    /**
     * Map network agent observation from Avro to entity
     */
    private PaymentHistory.AgentObservation mapNetworkAgentObservation(
            com.aml.payment.NetworkAgentObservationSnapshot source) {
        PaymentHistory.AgentObservation observation = new PaymentHistory.AgentObservation();
        observation.setAgentName(source.getAgentName().toString());
        observation.setIsSuspicious(source.getIsSuspicious());
        observation.setProbability(source.getProbability());
        observation.setRiskScore(source.getRiskScore());
        observation.setConfidence(source.getConfidence().toString());
        observation.setResponseTimeMs(source.getResponseTimeMs());
        return observation;
    }
}
