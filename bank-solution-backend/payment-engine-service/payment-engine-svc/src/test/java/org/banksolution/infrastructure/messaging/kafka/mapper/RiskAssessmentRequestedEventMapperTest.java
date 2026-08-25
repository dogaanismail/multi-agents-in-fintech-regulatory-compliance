package org.banksolution.infrastructure.messaging.kafka.mapper;

import com.aml.risk.PaymentType;
import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.domain.payment.event.RiskAssessmentInitiatedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.PaymentFixtures.*;

class RiskAssessmentRequestedEventMapperTest {

    @Test
    void shouldMapEveryFieldOfTheRiskRequest() {
        RiskAssessmentRequestedEvent riskAssessmentRequestedEvent =
                RiskAssessmentRequestedEventMapper.toAvroRequest(createRiskAssessmentInitiatedEvent());

        assertThat(riskAssessmentRequestedEvent.getPaymentId()).isEqualTo(PAYMENT_UUID.toString());
        assertThat(riskAssessmentRequestedEvent.getCustomerId()).isEqualTo(CUSTOMER_ID.toString());
        assertThat(riskAssessmentRequestedEvent.getSourceAccountId()).isEqualTo(SOURCE_ACCOUNT_ID.toString());
        assertThat(riskAssessmentRequestedEvent.getDestinationAccountId()).isEqualTo(DESTINATION_ACCOUNT_ID.toString());
        assertThat(riskAssessmentRequestedEvent.getAmount()).isEqualTo("100.00");
        assertThat(riskAssessmentRequestedEvent.getFromCurrency()).isEqualTo(FROM_CURRENCY);
        assertThat(riskAssessmentRequestedEvent.getToCurrency()).isEqualTo(TO_CURRENCY);
        assertThat(riskAssessmentRequestedEvent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
        assertThat(riskAssessmentRequestedEvent.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(riskAssessmentRequestedEvent.getTimestamp()).isPositive();
    }

    @Test
    void shouldLeaveAbsentAccountsAbsentForDepositsAndWithdrawals() {
        RiskAssessmentInitiatedEvent depositInitiatedEvent = new RiskAssessmentInitiatedEvent(
                createPaymentId(), CUSTOMER_ID, null, DESTINATION_ACCOUNT_ID, AMOUNT, FROM_CURRENCY, TO_CURRENCY, "DEPOSIT", null);
        RiskAssessmentInitiatedEvent withdrawalInitiatedEvent = new RiskAssessmentInitiatedEvent(
                createPaymentId(), CUSTOMER_ID, SOURCE_ACCOUNT_ID, null, AMOUNT, FROM_CURRENCY, TO_CURRENCY, "WITHDRAWAL", null);

        RiskAssessmentRequestedEvent depositRequestedEvent = RiskAssessmentRequestedEventMapper.toAvroRequest(depositInitiatedEvent);
        RiskAssessmentRequestedEvent withdrawalRequestedEvent = RiskAssessmentRequestedEventMapper.toAvroRequest(withdrawalInitiatedEvent);

        assertThat(depositRequestedEvent.getSourceAccountId()).isNull();
        assertThat(depositRequestedEvent.getPaymentType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(withdrawalRequestedEvent.getDestinationAccountId()).isNull();
        assertThat(withdrawalRequestedEvent.getPaymentType()).isEqualTo(PaymentType.WITHDRAWAL);
        assertThat(withdrawalRequestedEvent.getDescription()).isNull();
    }
}
