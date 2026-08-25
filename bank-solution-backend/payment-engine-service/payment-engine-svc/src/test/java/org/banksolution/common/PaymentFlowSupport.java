package org.banksolution.common;

import com.aml.feedback.ComplianceAgentManualFeedbackEvent;
import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.LedgerPostingRequestedEvent;
import com.aml.ledger.PostingInstructionType;
import com.aml.payment.PaymentCompletedEvent;
import com.aml.payment.PaymentCreatedEvent;
import com.aml.payment.PaymentSnapshotEvent;
import com.aml.risk.RiskAction;
import com.aml.risk.RiskAssessmentRequestedEvent;
import com.aml.risk.RiskLevel;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.enums.PaymentEventTrigger;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.banksolution.fixtures.AvroEventFixtures.createLedgerPostingCompletedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createPaymentCreatedEvent;
import static org.banksolution.fixtures.AvroEventFixtures.createRiskAssessmentCompletedEvent;

/**
 * Drives one payment through the Kafka contracts the way ledger-svc and risk-engine-svc do,
 * so flow tests read as the business conversation rather than as producer plumbing.
 */
public abstract class PaymentFlowSupport extends BaseIntegrationTest {

    protected static final Duration FLOW_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.kafka.topics.incoming.payment-created}")
    protected String paymentCreatedTopic;

    @Value("${spring.kafka.topics.incoming.risk-assessment-completed}")
    protected String riskAssessmentCompletedTopic;

    @Value("${spring.kafka.topics.incoming.ledger-posting-completed}")
    protected String ledgerPostingCompletedTopic;

    @Value("${spring.kafka.topics.outgoing.risk-assessment-requested}")
    protected String riskAssessmentRequestedTopic;

    @Value("${spring.kafka.topics.outgoing.payment-snapshot-events}")
    protected String paymentSnapshotTopic;

    @Value("${spring.kafka.topics.outgoing.ledger-posting-requested}")
    protected String ledgerPostingRequestedTopic;

    @Value("${spring.kafka.topics.outgoing.payment-completed}")
    protected String paymentCompletedTopic;

    @Value("${spring.kafka.topics.outgoing.agent-manual-feedback}")
    protected String agentManualFeedbackTopic;

    protected UUID givenPaymentCreated() throws ExecutionException, InterruptedException {
        UUID paymentId = UUID.randomUUID();
        publish(paymentCreatedTopic, paymentId, createPaymentCreatedEvent(paymentId));
        return paymentId;
    }

    protected UUID givenAuthorisedPayment() throws ExecutionException, InterruptedException {
        UUID paymentId = givenPaymentCreated();
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION);
        whenLedgerAnswers(paymentId, PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION, true, null);
        awaitRiskAssessmentRequested(paymentId);
        return paymentId;
    }

    protected void whenLedgerAnswers(
            UUID paymentId,
            PostingInstructionType postingInstructionType,
            boolean success,
            String failureReason) throws ExecutionException, InterruptedException {

        publish(ledgerPostingCompletedTopic, paymentId, createLedgerPostingCompletedEvent(
                paymentId, postingInstructionType, success, success ? UUID.randomUUID() : null, failureReason));
    }

    protected void whenRiskEngineDecides(UUID paymentId, RiskAction riskAction)
            throws ExecutionException, InterruptedException {

        RiskLevel riskLevel = switch (riskAction) {
            case PROCEED -> RiskLevel.LOW;
            case ESCALATE -> RiskLevel.MEDIUM;
            case BLOCK -> RiskLevel.HIGH;
        };
        publish(riskAssessmentCompletedTopic, paymentId,
                createRiskAssessmentCompletedEvent(paymentId, riskAction, riskLevel, 0.5));
    }

    protected LedgerPostingRequestedEvent awaitLedgerPostingRequested(
            UUID paymentId,
            PostingInstructionType postingInstructionType) {

        return KafkaTestClients.awaitMatchingEvent(ledgerPostingRequestedTopic, FLOW_TIMEOUT,
                (LedgerPostingRequestedEvent ledgerPostingRequestedEvent) ->
                        paymentId.toString().equals(ledgerPostingRequestedEvent.getClientTransactionId())
                                && ledgerPostingRequestedEvent.getPostingInstructionType() == postingInstructionType);
    }

    protected RiskAssessmentRequestedEvent awaitRiskAssessmentRequested(UUID paymentId) {
        return KafkaTestClients.awaitMatchingEvent(riskAssessmentRequestedTopic, FLOW_TIMEOUT,
                (RiskAssessmentRequestedEvent riskAssessmentRequestedEvent) ->
                        paymentId.toString().equals(riskAssessmentRequestedEvent.getPaymentId()));
    }

    protected PaymentCompletedEvent awaitPaymentCompleted(UUID paymentId) {
        return KafkaTestClients.awaitMatchingEvent(paymentCompletedTopic, FLOW_TIMEOUT,
                (PaymentCompletedEvent paymentCompletedEvent) ->
                        paymentId.toString().equals(paymentCompletedEvent.getPaymentId()));
    }

    protected PaymentSnapshotEvent awaitSnapshot(UUID paymentId, PaymentEventTrigger paymentEventTrigger) {
        return KafkaTestClients.awaitMatchingEvent(paymentSnapshotTopic, FLOW_TIMEOUT,
                (PaymentSnapshotEvent paymentSnapshotEvent) ->
                        paymentId.toString().equals(paymentSnapshotEvent.getPaymentId())
                                && paymentEventTrigger.name().equals(paymentSnapshotEvent.getEventTrigger()));
    }

    protected ComplianceAgentManualFeedbackEvent awaitComplianceFeedback(UUID paymentId) {
        return KafkaTestClients.awaitMatchingEvent(agentManualFeedbackTopic, FLOW_TIMEOUT,
                (ComplianceAgentManualFeedbackEvent complianceAgentManualFeedbackEvent) ->
                        paymentId.toString().equals(complianceAgentManualFeedbackEvent.getPaymentId()));
    }

    protected <T extends SpecificRecord> void publish(
            String topic,
            UUID key,
            T avroEvent) throws ExecutionException, InterruptedException {

        try (KafkaProducer<String, T> producer = KafkaTestClients.createAvroProducer()) {
            producer.send(new ProducerRecord<>(topic, key.toString(), avroEvent)).get();
        }
    }

    protected static PaymentCreatedEvent createPaymentCreatedEventFor(UUID paymentId) {
        return createPaymentCreatedEvent(paymentId);
    }

    protected static LedgerPostingCompletedEvent createLedgerOutcome(
            UUID paymentId,
            PostingInstructionType postingInstructionType) {

        return createLedgerPostingCompletedEvent(paymentId, postingInstructionType, true, UUID.randomUUID(), null);
    }
}
