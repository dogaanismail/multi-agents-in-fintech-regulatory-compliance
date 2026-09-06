package org.banksolution.infrastructure.messaging.kafka;

import com.aml.ledger.PostingInstructionType;
import com.aml.risk.RiskAction;
import org.banksolution.common.PaymentFlowSupport;
import org.banksolution.domain.payment.event.RiskAssessmentCompletedEvent;
import org.banksolution.enums.PaymentEventTrigger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RiskAssessmentCompletionFlowTest extends PaymentFlowSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSequenceTheCompletionInThePaymentStreamInsteadOfStoringItDetached() throws Exception {
        UUID paymentId = givenAuthorisedPayment();

        whenRiskEngineDecides(paymentId, RiskAction.PROCEED);
        awaitLedgerPostingRequested(paymentId, PostingInstructionType.SETTLEMENT);

        assertThat(storedPayloadTypesOf(paymentId))
                .containsSubsequence(
                        "org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent",
                        RiskAssessmentCompletedEvent.class.getName(),
                        "org.banksolution.domain.payment.event.FraudCheckApprovedEvent");
        assertThat(detachedCompletionEventsMentioning(paymentId)).isZero();
    }

    @Test
    void shouldActOnARedeliveredCompletionOnlyOnce() throws Exception {
        UUID paymentId = givenAuthorisedPayment();

        whenRiskEngineDecides(paymentId, RiskAction.PROCEED);
        awaitSnapshot(paymentId, PaymentEventTrigger.RISK_ASSESSMENT_COMPLETED);
        whenRiskEngineDecides(paymentId, RiskAction.BLOCK);

        whenLedgerAnswers(paymentId, PostingInstructionType.SETTLEMENT, true, null);
        awaitPaymentCompleted(paymentId);

        assertThat(storedPayloadTypesOf(paymentId))
                .containsOnlyOnce(RiskAssessmentCompletedEvent.class.getName())
                .doesNotContain("org.banksolution.domain.payment.event.PaymentBlockedEvent");
    }

    private List<String> storedPayloadTypesOf(UUID paymentId) {
        return jdbcTemplate.queryForList(
                "select payload_type from domain_event_entry where aggregate_identifier = ? order by sequence_number",
                String.class,
                paymentId.toString());
    }

    private Integer detachedCompletionEventsMentioning(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from domain_event_entry where aggregate_identifier = event_identifier "
                        + "and payload_type = ? and convert_from(payload, 'UTF8') like ?",
                Integer.class,
                RiskAssessmentCompletedEvent.class.getName(),
                "%" + paymentId + "%");
    }
}
