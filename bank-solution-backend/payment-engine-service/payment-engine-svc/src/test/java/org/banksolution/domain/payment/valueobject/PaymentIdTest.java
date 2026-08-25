package org.banksolution.domain.payment.valueobject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentIdTest {

    @Test
    void shouldBeEqualByIdentifierOnly() {
        UUID identifier = UUID.randomUUID();
        PaymentId paymentId = new PaymentId(identifier);
        PaymentId samePaymentId = new PaymentId(identifier);

        assertThat(paymentId).isEqualTo(samePaymentId).hasSameHashCodeAs(samePaymentId).isEqualTo(paymentId);
        assertThat(paymentId).isNotEqualTo(new PaymentId()).isNotEqualTo(null).isNotEqualTo(identifier);
        assertThat(paymentId.toString()).isEqualTo(identifier.toString());
        assertThat(paymentId.getIdentifier()).isEqualTo(identifier);
    }

    @Test
    void shouldGenerateADistinctIdentifierWhenNoneIsGiven() {
        assertThat(new PaymentId().getIdentifier()).isNotNull().isNotEqualTo(new PaymentId().getIdentifier());
    }

    @Test
    void shouldRoundTripThroughJsonUnderTheIdentifierProperty() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PaymentId paymentId = new PaymentId();

        String json = objectMapper.writeValueAsString(paymentId);

        assertThat(json).isEqualTo("{\"identifier\":\"" + paymentId.getIdentifier() + "\"}");
        assertThat(objectMapper.readValue(json, PaymentId.class)).isEqualTo(paymentId);
    }
}
