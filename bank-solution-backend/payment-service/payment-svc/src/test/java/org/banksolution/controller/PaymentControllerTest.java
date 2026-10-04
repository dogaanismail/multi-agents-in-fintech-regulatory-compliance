package org.banksolution.controller;

import com.aml.payment.PaymentCreatedEvent;
import com.aml.payment.PaymentScheme;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.common.kafka.KafkaTestClients;
import org.banksolution.domain.PaymentIdempotency;
import org.banksolution.enums.Currency;
import org.banksolution.model.request.PaymentRequest;
import org.banksolution.model.response.PaymentRequestResponse;
import org.banksolution.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.banksolution.common.initializers.WireMockInitializer.ACCOUNT_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.PaymentFixtures.*;
import static org.banksolution.http.IdempotencyHeaders.IDEMPOTENCY_KEY;
import static org.banksolution.http.IdempotencyHeaders.IDEMPOTENT_REPLAYED;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends BaseIntegrationTest {

    private static final String PAYMENTS_URL = "/api/v1/payments";
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(30);

    @Value("${spring.kafka.topics.outgoing.payment-created}")
    private String paymentCreatedTopic;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

        MvcResult mvcResult = mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
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
        await().atMost(EVENT_TIMEOUT).untilAsserted(() ->
                assertThat(findOutboxEventStatusesByPaymentId(paymentRequestResponse.getId())).containsExactly("PROCESSED"));
    }

    @Test
    void shouldBookADepositAsExternalInboundWithoutConversion() throws Exception {
        UUID customerId = UUID.randomUUID();

        MvcResult mvcResult = mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
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

        mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source account is required for TRANSFER_OUT"));
    }

    @Test
    void shouldRejectAPaymentWhoseRateIsUnknownAsUnprocessable() throws Exception {
        givenAccountServiceKnows(List.of());

        mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTransferOutRequest(UUID.randomUUID(), Currency.GBP, Currency.NGN))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("No exchange rate available for GBP to NGN"));
    }

    @Test
    void shouldRejectARequestThatFailsBeanValidation() throws Exception {
        PaymentRequest paymentRequest = createDepositRequest(null);
        paymentRequest.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.subErrors.length()").value(2));
    }

    @Test
    void shouldReturnTheOriginalPaymentWhenARequestIsRetriedWithTheSameKey() throws Exception {
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        String depositRequestJson = objectMapper.writeValueAsString(createDepositRequest(customerId));

        MvcResult originalMvcResult = performPaymentRequest(idempotencyKey, depositRequestJson)
                .andExpect(status().isCreated())
                .andExpect(header().string(IDEMPOTENT_REPLAYED, "false"))
                .andReturn();
        MvcResult retriedMvcResult = performPaymentRequest(idempotencyKey, depositRequestJson)
                .andExpect(status().isCreated())
                .andExpect(header().string(IDEMPOTENT_REPLAYED, "true"))
                .andReturn();

        UUID originalPaymentId = readPaymentId(originalMvcResult);
        assertThat(readPaymentId(retriedMvcResult)).isEqualTo(originalPaymentId);
        assertThat(countPaymentRequestsByCustomerId(customerId)).isOne();
        await().atMost(EVENT_TIMEOUT).untilAsserted(() ->
                assertThat(findOutboxEventStatusesByPaymentId(originalPaymentId)).containsExactly("PROCESSED"));
    }

    @Test
    void shouldRejectAKeyReusedForADifferentPaymentAsUnprocessable() throws Exception {
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest changedDepositRequest = createDepositRequest(customerId);
        changedDepositRequest.setAmount(new BigDecimal("250.00"));

        performPaymentRequest(idempotencyKey, objectMapper.writeValueAsString(createDepositRequest(customerId)))
                .andExpect(status().isCreated());
        performPaymentRequest(idempotencyKey, objectMapper.writeValueAsString(changedDepositRequest))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("different request")));

        assertThat(countPaymentRequestsByCustomerId(customerId)).isOne();
    }

    @Test
    void shouldRejectAPaymentRequestWithoutAnIdempotencyKey() throws Exception {
        mockMvc.perform(post(PAYMENTS_URL + "/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDepositRequest(UUID.randomUUID()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Idempotency-Key header is required"));
    }

    @Test
    void shouldCreateExactlyOnePaymentWhenConcurrentRetriesRaceWithTheSameKey() throws Exception {
        UUID customerId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        String depositRequestJson = objectMapper.writeValueAsString(createDepositRequest(customerId));
        int concurrentRetries = 8;

        List<MvcResult> concurrentMvcResults;
        try (ExecutorService concurrentRetryExecutor = Executors.newFixedThreadPool(concurrentRetries)) {
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Future<MvcResult>> pendingMvcResults = IntStream.range(0, concurrentRetries)
                    .mapToObj(_ -> concurrentRetryExecutor.submit(() -> {
                        startSignal.await();
                        return performPaymentRequest(idempotencyKey, depositRequestJson).andReturn();
                    }))
                    .toList();
            startSignal.countDown();
            concurrentMvcResults = new ArrayList<>();
            for (Future<MvcResult> pendingMvcResult : pendingMvcResults) {
                concurrentMvcResults.add(pendingMvcResult.get());
            }
        }

        assertThat(concurrentMvcResults).allMatch(mvcResult -> mvcResult.getResponse().getStatus() == HttpStatus.CREATED.value());
        assertThat(concurrentMvcResults).extracting(this::readPaymentId).containsOnly(PaymentIdempotency.derivePaymentId(customerId, idempotencyKey));
        assertThat(concurrentMvcResults)
                .filteredOn(mvcResult -> "false".equals(mvcResult.getResponse().getHeader(IDEMPOTENT_REPLAYED)))
                .hasSize(1);
        assertThat(countPaymentRequestsByCustomerId(customerId)).isOne();
        await().atMost(EVENT_TIMEOUT).untilAsserted(() -> assertThat(findOutboxEventStatusesByPaymentId(
                PaymentIdempotency.derivePaymentId(customerId, idempotencyKey))).containsExactly("PROCESSED"));
    }

    @Test
    void shouldListOnlyTheCustomersPayments() throws Exception {
        UUID customerId = UUID.randomUUID();
        mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepositRequest(customerId)))).andExpect(status().isCreated());
        mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepositRequest(customerId)))).andExpect(status().isCreated());
        mockMvc.perform(post(PAYMENTS_URL + "/request").header(IDEMPOTENCY_KEY, UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDepositRequest(UUID.randomUUID())))).andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(PAYMENTS_URL + "/customer/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].message").doesNotExist());
    }

    private ResultActions performPaymentRequest(String idempotencyKey, String paymentRequestJson) throws Exception {
        return mockMvc.perform(post(PAYMENTS_URL + "/request")
                .header(IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequestJson));
    }

    private UUID readPaymentId(MvcResult mvcResult) {
        return objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), PaymentRequestResponse.class).getId();
    }

    private Integer countPaymentRequestsByCustomerId(UUID customerId) {
        return jdbcTemplate.queryForObject("select count(*) from payment_request where customer_id = ?", Integer.class, customerId);
    }

    private void givenAccountServiceKnows(List<?> accountResponses) throws Exception {
        stubFor(get(urlPathEqualTo(ACCOUNT_SERVICE_BASE_PATH + "/ids"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(accountResponses))));
    }

    private List<String> findOutboxEventStatusesByPaymentId(UUID paymentId) {
        return jdbcTemplate.queryForList(
                "select status from outbox_event where reference_id = ?",
                String.class,
                paymentId.toString());
    }
}
