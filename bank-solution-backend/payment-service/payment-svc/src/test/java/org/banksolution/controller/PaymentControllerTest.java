package org.banksolution.controller;

import com.aml.payment.PaymentCreatedEvent;
import com.aml.payment.PaymentScheme;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.enums.Currency;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.common.initializers.WireMockInitializer.ACCOUNT_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends BaseIntegrationTest {

    private static final String PAYMENTS_URL = "/api/v1/payments";
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.kafka.topics.outgoing.payment-created}")
    private String paymentCreatedTopic;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @BeforeEach
    void givenGbpToEurRate() {
        if (exchangeRateRepository.findByCurrencyPair("GBPEUR").isEmpty()) {
            exchangeRateRepository.saveAndFlush(createExchangeRateEntity(Currency.GBP, Currency.EUR, "1.16000000"));
        }
    }

    @Test
    void shouldConvertBookAndPublishACrossBorderInternalTransfer() throws Exception {
        UUID customerId = UUID.randomUUID();
        givenAccountServiceKnows(List.of(createAccountResponse(SOURCE_ACCOUNT_ID, "GB"), createAccountResponse(DESTINATION_ACCOUNT_ID, "DE")));

        MvcResult mvcResult = mockMvc.perform(post(PAYMENTS_URL + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransferOutRequest(customerId, Currency.GBP, Currency.EUR))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.convertedAmount").value(116.00))
                .andExpect(jsonPath("$.appliedExchangeRate").value(1.16))
                .andExpect(jsonPath("$.toCurrency").value("EUR"))
                .andExpect(jsonPath("$.message").value("Payment request submitted successfully and is being processed"))
                .andReturn();

        PaymentRequestResponse paymentRequestResponse =
                objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PaymentRequestResponse.class);
        PaymentCreatedEvent paymentCreatedEvent = KafkaTestClients.awaitMatchingEvent(paymentCreatedTopic, EVENT_TIMEOUT,
                (PaymentCreatedEvent publishedEvent) -> paymentRequestResponse.getId().toString().equals(publishedEvent.getPaymentId()));
        assertThat(paymentCreatedEvent.getPaymentScheme()).isEqualTo(PaymentScheme.INTERNAL_TRANSFER);
        assertThat(paymentCreatedEvent.getIsCrossBorderPayment()).isTrue();
        assertThat(paymentCreatedEvent.getConvertedAmount()).isEqualTo("116.00");
        assertThat(paymentCreatedEvent.getAppliedExchangeRate()).isEqualTo("1.16000000");
    }

    @Test
    void shouldBookADepositAsExternalInboundWithoutConversion() throws Exception {
        UUID customerId = UUID.randomUUID();

        MvcResult mvcResult = mockMvc.perform(post(PAYMENTS_URL + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDepositRequest(customerId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.appliedExchangeRate").doesNotExist())
                .andReturn();

        PaymentRequestResponse paymentRequestResponse =
                objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PaymentRequestResponse.class);
        PaymentCreatedEvent paymentCreatedEvent = KafkaTestClients.awaitMatchingEvent(paymentCreatedTopic, EVENT_TIMEOUT,
                (PaymentCreatedEvent publishedEvent) -> paymentRequestResponse.getId().toString().equals(publishedEvent.getPaymentId()));
        assertThat(paymentCreatedEvent.getPaymentScheme()).isEqualTo(PaymentScheme.EXTERNAL_INBOUND);
        assertThat(paymentCreatedEvent.getIsCrossBorderPayment()).isFalse();
        assertThat(paymentCreatedEvent.getSourceAccountId()).isNull();
    }

    @Test
    void shouldRejectADebitWithoutASourceAccountAsABadRequest() throws Exception {
        PaymentRequest paymentRequest = createTransferOutRequest(UUID.randomUUID(), Currency.GBP, Currency.GBP);
        paymentRequest.setSourceAccountId(null);

        mockMvc.perform(post(PAYMENTS_URL + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source account is required for TRANSFER_OUT"));
    }

    @Test
    void shouldRejectAPaymentWhoseRateIsUnknownAsUnprocessable() throws Exception {
        givenAccountServiceKnows(List.of());

        mockMvc.perform(post(PAYMENTS_URL + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransferOutRequest(UUID.randomUUID(), Currency.GBP, Currency.NGN))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("No exchange rate available for GBP to NGN"));
    }

    @Test
    void shouldRejectARequestThatFailsBeanValidation() throws Exception {
        PaymentRequest paymentRequest = createDepositRequest(null);
        paymentRequest.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post(PAYMENTS_URL + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.subErrors.length()").value(2));
    }

    @Test
    void shouldListOnlyTheCustomersPayments() throws Exception {
        UUID customerId = UUID.randomUUID();
        mockMvc.perform(post(PAYMENTS_URL + "/request").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepositRequest(customerId)))).andExpect(status().isCreated());
        mockMvc.perform(post(PAYMENTS_URL + "/request").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepositRequest(customerId)))).andExpect(status().isCreated());
        mockMvc.perform(post(PAYMENTS_URL + "/request").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepositRequest(UUID.randomUUID())))).andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(PAYMENTS_URL + "/customer/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].message").doesNotExist());
    }

    private void givenAccountServiceKnows(List<?> accountResponses) throws Exception {
        stubFor(get(urlPathEqualTo(ACCOUNT_SERVICE_BASE_PATH + "/ids"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(accountResponses))));
    }
}
