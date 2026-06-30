package org.banksolution.infrastructure.messaging.kafka.producer;

import com.aml.account.AccountChargeRequestedEvent;
import com.aml.account.PaymentType;
import org.banksolution.config.KafkaConfigurationProperties;
import org.banksolution.fixtures.PaymentFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AccountChargeRequestedEventProducerTest {

    private KafkaConfigurationProperties properties;
    private KafkaTemplate<String, AccountChargeRequestedEvent> template;
    private AccountChargeRequestedEventProducer producer;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        properties = new KafkaConfigurationProperties();
        properties.getTopics().getOutgoing().setAccountChargeRequested("account.charge.requested");
        template = mock(KafkaTemplate.class);
        producer = new AccountChargeRequestedEventProducer(properties, template);
    }

    @Test
    void shouldPublishMappedAvroRequestToConfiguredTopic() {
        producer.publishAccountChargeRequestedEvent(PaymentFixtures.createAccountChargeInitiatedEvent());

        ArgumentCaptor<AccountChargeRequestedEvent> captor = ArgumentCaptor.forClass(AccountChargeRequestedEvent.class);
        verify(template).send(eq("account.charge.requested"), eq(PaymentFixtures.PAYMENT_UUID.toString()), captor.capture());

        AccountChargeRequestedEvent sent = captor.getValue();
        assertThat(sent.getPaymentId()).isEqualTo(PaymentFixtures.PAYMENT_UUID.toString());
        assertThat(sent.getCustomerId()).isEqualTo(PaymentFixtures.CUSTOMER_ID.toString());
        assertThat(sent.getAmount()).isEqualTo(PaymentFixtures.AMOUNT.toString());
        assertThat(sent.getConvertedAmount()).isEqualTo(PaymentFixtures.CONVERTED_AMOUNT.toString());
        assertThat(sent.getPaymentType()).isEqualTo(PaymentType.TRANSFER_OUT);
    }
}
