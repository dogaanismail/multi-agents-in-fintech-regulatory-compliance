package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.PostingInstructionType;
import com.aml.payment.PaymentSnapshotEvent;
import com.aml.risk.RiskAction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.domain.payment.query.PaymentResponse;
import org.banksolution.domain.payment.service.PaymentQueryService;
import org.banksolution.domain.payment.valueobject.PaymentId;
import org.banksolution.enums.PaymentEventTrigger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The snapshot version is the payment's event-store sequence number, read from the loaded
 * aggregate. Snapshots are built by a tracking processor from a fresh load, so a snapshot may
 * already include later events than the one that triggered it — the version therefore never
 * decreases for one payment, and the last one equals the highest sequence number stored.
 */
class PaymentSnapshotVersionTest extends PaymentFlowSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PaymentQueryService paymentQueryService;

    @Test
    void shouldPublishTheEventStoreSequenceNumberAsAMonotonicSnapshotVersion() throws Exception {
        UUID paymentId = givenAuthorisedPayment();
        whenRiskEngineDecides(paymentId, RiskAction.PROCEED);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.SETTLEMENT);
        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, true, null);
        awaitSnapshot(paymentId, PaymentEventTrigger.PAYMENT_COMPLETED);

        List<PaymentSnapshotEvent> publishedSnapshots = collectSnapshotsInPublishOrder(paymentId);
        long highestStoredSequenceNumber = highestStoredSequenceNumber(paymentId);

        assertThat(publishedSnapshots)
                .extracting(PaymentSnapshotEvent::getEventTrigger)
                .startsWith(PaymentEventTrigger.PAYMENT_INITIATED.name())
                .endsWith(PaymentEventTrigger.PAYMENT_COMPLETED.name());

        List<Integer> snapshotVersions = publishedSnapshots.stream().map(PaymentSnapshotEvent::getVersion).toList();
        assertThat(snapshotVersions).isSorted();
        assertThat(snapshotVersions.getFirst()).isGreaterThanOrEqualTo(1);
        assertThat(snapshotVersions.getLast()).isEqualTo((int) highestStoredSequenceNumber);
        // 8 events: initiated, authorisation initiated, authorised, risk initiated, fraud approved,
        // settlement initiated, settled, completed — sequence numbers start at 0
        assertThat(highestStoredSequenceNumber).isEqualTo(7);

        PaymentResponse paymentResponse = paymentQueryService.findPaymentById(new PaymentId(paymentId));
        assertThat(paymentResponse.version()).isEqualTo(highestStoredSequenceNumber);
    }

    private long highestStoredSequenceNumber(UUID paymentId) {
        Long highestSequenceNumber = jdbcTemplate.queryForObject(
                "select max(sequence_number) from domain_event_entry where aggregate_identifier = ?",
                Long.class,
                paymentId.toString());

        assertThat(highestSequenceNumber).isNotNull();
        return highestSequenceNumber;
    }

    private List<PaymentSnapshotEvent> collectSnapshotsInPublishOrder(UUID paymentId) {
        List<PaymentSnapshotEvent> publishedSnapshots = new ArrayList<>();
        try (KafkaConsumer<String, Object> consumer = KafkaTestClients.createAvroConsumer(paymentSnapshotTopic)) {
            long deadline = System.currentTimeMillis() + FLOW_TIMEOUT.toMillis();
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, Object> consumedRecord : consumer.poll(Duration.ofMillis(500))) {
                    PaymentSnapshotEvent paymentSnapshotEvent = (PaymentSnapshotEvent) consumedRecord.value();
                    if (paymentId.toString().equals(paymentSnapshotEvent.getPaymentId())) {
                        publishedSnapshots.add(paymentSnapshotEvent);
                        if (PaymentEventTrigger.PAYMENT_COMPLETED.name().equals(paymentSnapshotEvent.getEventTrigger())) {
                            return publishedSnapshots;
                        }
                    }
                }
            }
        }
        throw new AssertionError("Never saw the PAYMENT_COMPLETED snapshot for " + paymentId);
    }
}
