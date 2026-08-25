package org.banksolution.mapper;

import com.aml.risk.RiskAssessmentRequestedEvent;
import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.enums.PaymentType;
import org.banksolution.enums.RiskCheckStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.AMOUNT;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createRiskAssessmentRequestedEvent;

class RiskCheckRequestEntityMapperTest {

    private static final String PAYMENT_ID = "PAY-1";

    @Test
    void shouldCopyTheIdentifiersAndPaymentDetails() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);

        RiskCheckRequestEntity entity = RiskCheckRequestEntityMapper.toEntity(event);

        assertThat(entity.getPaymentId()).isEqualTo(event.getPaymentId());
        assertThat(entity.getCustomerId()).isEqualTo(event.getCustomerId());
        assertThat(entity.getSourceAccountId()).isEqualTo(event.getSourceAccountId());
        assertThat(entity.getDestinationAccountId()).isEqualTo(event.getDestinationAccountId());
        assertThat(entity.getFromCurrency()).isEqualTo(event.getFromCurrency());
        assertThat(entity.getToCurrency()).isEqualTo(event.getToCurrency());
        assertThat(entity.getDescription()).isEqualTo(event.getDescription());
        assertThat(entity.getRequestTimestamp()).isEqualTo(event.getTimestamp());
    }

    @Test
    void shouldConvertTheStringAmountToABigDecimalWithoutLosingPrecision() {
        RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(PAYMENT_ID);

        RiskCheckRequestEntity entity = RiskCheckRequestEntityMapper.toEntity(event);

        assertThat(entity.getAmount()).isEqualByComparingTo(AMOUNT);
    }

    @Test
    void shouldMapEveryAvroPaymentTypeOntoTheDomainEnum() {
        for (com.aml.risk.PaymentType avroPaymentType : com.aml.risk.PaymentType.values()) {
            RiskAssessmentRequestedEvent event = createRiskAssessmentRequestedEvent(
                    PAYMENT_ID, avroPaymentType, "source", "destination");

            RiskCheckRequestEntity entity = RiskCheckRequestEntityMapper.toEntity(event);

            assertThat(entity.getPaymentType()).isEqualTo(PaymentType.valueOf(avroPaymentType.name()));
        }
    }

    @Test
    void shouldStartEveryRequestAwaitingMarl() {
        RiskCheckRequestEntity entity =
                RiskCheckRequestEntityMapper.toEntity(createRiskAssessmentRequestedEvent(PAYMENT_ID));

        assertThat(entity.getStatus()).isEqualTo(RiskCheckStatus.AWAITING_MARL);
        assertThat(entity.getId()).isNull();
    }
}
