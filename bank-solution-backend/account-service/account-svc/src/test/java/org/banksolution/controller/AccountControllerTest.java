package org.banksolution.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.repository.AccountRepository;
import org.banksolution.repository.AccountWalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.common.initializers.WireMockInitializer.CUSTOMER_SERVICE_BASE_PATH;
import static org.banksolution.common.initializers.WireMockInitializer.LEDGER_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.AccountFixtures.createAccountEntity;
import static org.banksolution.fixtures.AccountFixtures.createAccountWalletEntity;
import static org.banksolution.fixtures.AccountFixtures.createCustomerResponse;
import static org.banksolution.fixtures.AccountFixtures.createLedgerAccountResponse;
import static org.banksolution.fixtures.AccountFixtures.createOpenAccountRequest;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerTest extends BaseIntegrationTest {

    private static final String ACCOUNTS_URL = "/api/v1/accounts";
    private static final String OPEN_ACCOUNT_URL = ACCOUNTS_URL + "/open-account";
    private static final String LEDGER_BATCH_URL = LEDGER_SERVICE_BASE_PATH + "/batch";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountWalletRepository accountWalletRepository;

    @Test
    void shouldOpenAnAccountWithALedgerBackedWalletPerCurrency() throws Exception {
        UUID customerId = UUID.randomUUID();
        givenCustomerExists(customerId);
        givenLedgerOpensAccountsFor(List.of(Currency.GBP, Currency.JPY));

        MvcResult mvcResult = mockMvc.perform(post(OPEN_ACCOUNT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createOpenAccountRequest(customerId, List.of(Currency.GBP, Currency.JPY)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.bankLocation").value("GB"))
                .andExpect(jsonPath("$.accountType").value("CHECKING"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.openingDate").isNotEmpty())
                .andExpect(jsonPath("$.wallets.length()").value(2))
                .andExpect(jsonPath("$.wallets[0].currency").value("GBP"))
                .andExpect(jsonPath("$.wallets[0].primary").value(true))
                .andExpect(jsonPath("$.wallets[1].currency").value("JPY"))
                .andExpect(jsonPath("$.wallets[1].primary").value(false))
                .andReturn();

        AccountResponse accountResponse = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(), AccountResponse.class);
        assertThat(accountRepository.findActiveById(accountResponse.getId())).isPresent();
        assertThat(accountWalletRepository.findByAccountId(accountResponse.getId()))
                .extracting(AccountWalletEntity::getCurrency)
                .containsExactlyInAnyOrder(Currency.GBP, Currency.JPY);
        verify(postRequestedFor(urlEqualTo(LEDGER_BATCH_URL)));
    }

    @Test
    void shouldRefuseToOpenAnAccountForACustomerTheCustomerServiceDoesNotKnow() throws Exception {
        UUID unknownCustomerId = UUID.randomUUID();
        stubFor(WireMock.get(urlEqualTo(CUSTOMER_SERVICE_BASE_PATH + "/" + unknownCustomerId))
                .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(post(OPEN_ACCOUNT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createOpenAccountRequest(unknownCustomerId, List.of(Currency.GBP)))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found with id: " + unknownCustomerId));

        assertThat(accountRepository.findActiveByCustomerId(unknownCustomerId)).isEmpty();
    }

    @Test
    void shouldReportTheLedgerBeingUnavailableAndNotLeaveAnAccountBehind() throws Exception {
        UUID customerId = UUID.randomUUID();
        givenCustomerExists(customerId);
        stubFor(WireMock.post(urlEqualTo(LEDGER_BATCH_URL)).willReturn(aResponse().withStatus(503)));

        mockMvc.perform(post(OPEN_ACCOUNT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createOpenAccountRequest(customerId, List.of(Currency.GBP)))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(startsWith("Could not open ledger wallets")));

        assertThat(accountRepository.findActiveByCustomerId(customerId)).isEmpty();
    }

    @Test
    void shouldRejectAnOpenRequestThatFailsBeanValidation() throws Exception {
        OpenAccountRequest openAccountRequest = createOpenAccountRequest(null, List.of());

        mockMvc.perform(post(OPEN_ACCOUNT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(openAccountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.subErrors.length()").value(2));
    }

    @Test
    void shouldReturnTheAccountWithItsWalletsById() throws Exception {
        AccountEntity accountEntity = givenPersistedAccountWithWallets(UUID.randomUUID(), Currency.EUR);

        mockMvc.perform(get(
                        ACCOUNTS_URL + "/" + accountEntity.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountEntity.getId().toString()))
                .andExpect(jsonPath("$.accountNumber").value(accountEntity.getAccountNumber()))
                .andExpect(jsonPath("$.wallets.length()").value(1))
                .andExpect(jsonPath("$.wallets[0].currency").value("EUR"));
    }

    @Test
    void shouldNotReturnASoftDeletedAccount() throws Exception {
        AccountEntity accountEntity = givenPersistedAccountWithWallets(UUID.randomUUID(), Currency.EUR);
        accountEntity.setDeletedAt(Instant.now());
        accountRepository.saveAndFlush(accountEntity);

        mockMvc.perform(get(
                        ACCOUNTS_URL + "/" + accountEntity.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with id: " + accountEntity.getId()));
    }

    @Test
    void shouldReturnOnlyTheRequestedAccountIds() throws Exception {
        UUID customerId = UUID.randomUUID();
        AccountEntity firstAccountEntity = givenPersistedAccountWithWallets(customerId, Currency.GBP);
        AccountEntity secondAccountEntity = givenPersistedAccountWithWallets(customerId, Currency.GBP);
        givenPersistedAccountWithWallets(customerId, Currency.GBP);

        mockMvc.perform(get(ACCOUNTS_URL + "/ids")
                        .param("ids", firstAccountEntity.getId().toString(), secondAccountEntity.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(
                        firstAccountEntity.getId().toString(), secondAccountEntity.getId().toString())));
    }

    @Test
    void shouldReturnEveryAccountOfTheCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        givenPersistedAccountWithWallets(customerId, Currency.GBP);
        givenPersistedAccountWithWallets(customerId, Currency.USD);

        mockMvc.perform(get(
                        ACCOUNTS_URL + "/customer/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].customerId").value(everyItem(
                        is(customerId.toString()))));
    }

    @Test
    void shouldReturnTheWalletsOfTheAccount() throws Exception {
        AccountEntity accountEntity = givenPersistedAccountWithWallets(UUID.randomUUID(), Currency.GBP, Currency.JPY);

        mockMvc.perform(get(
                        ACCOUNTS_URL + "/" + accountEntity.getId() + "/wallets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].currency").value(containsInAnyOrder("GBP", "JPY")));
    }

    @Test
    void shouldReturnTheWalletForTheRequestedCurrency() throws Exception {
        AccountEntity accountEntity = givenPersistedAccountWithWallets(UUID.randomUUID(), Currency.GBP, Currency.JPY);

        mockMvc.perform(get(
                        ACCOUNTS_URL + "/" + accountEntity.getId() + "/wallets/JPY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("JPY"))
                .andExpect(jsonPath("$.primary").value(false))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.availableBalance").value(0));
    }

    @Test
    void shouldReportAMissingWalletForACurrencyTheAccountDoesNotHold() throws Exception {
        AccountEntity accountEntity = givenPersistedAccountWithWallets(UUID.randomUUID(), Currency.GBP);

        mockMvc.perform(get(
                        ACCOUNTS_URL + "/" + accountEntity.getId() + "/wallets/JPY"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Wallet not found for account: " + accountEntity.getId() + " and currency: JPY"));
    }

    private void givenCustomerExists(UUID customerId) {
        stubFor(WireMock.get(urlEqualTo(CUSTOMER_SERVICE_BASE_PATH + "/" + customerId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(createCustomerResponse(customerId)))));
    }

    private void givenLedgerOpensAccountsFor(List<Currency> currencies) {
        List<LedgerAccountResponse> ledgerAccountResponses = currencies.stream()
                .map(currency -> createLedgerAccountResponse(null, currency))
                .toList();
        stubFor(WireMock.post(urlEqualTo(LEDGER_BATCH_URL))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(ledgerAccountResponses))));
    }

    private AccountEntity givenPersistedAccountWithWallets(UUID customerId, Currency... currencies) {
        AccountEntity accountEntity = accountRepository.saveAndFlush(createAccountEntity(customerId));
        for (int currencyIndex = 0; currencyIndex < currencies.length; currencyIndex++) {
            accountWalletRepository.saveAndFlush(
                    createAccountWalletEntity(accountEntity, currencies[currencyIndex], currencyIndex == 0));
        }
        return accountEntity;
    }
}
