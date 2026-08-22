package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.banksolution.fixtures.LedgerAccountFixtures.createLedgerAccountRequest;
import static org.banksolution.fixtures.LedgerAccountFixtures.createLedgerAccountsRequest;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerAccountControllerTest extends BaseIntegrationTest {

    private static final String LEDGER_ACCOUNTS_URL = "/api/v1/ledger/accounts";
    private static final String BATCH_URL = LEDGER_ACCOUNTS_URL + "/batch";
    private static final String BY_ID_URL = LEDGER_ACCOUNTS_URL + "/{ledgerAccountId}";
    private static final String BY_BANK_ACCOUNT_URL = LEDGER_ACCOUNTS_URL + "/bank-account/{accountId}";
    private static final String BY_BANK_ACCOUNT_AND_CURRENCY_URL = BY_BANK_ACCOUNT_URL + "/{currency}";

    private static final String LEDGER_ACCOUNT_ID = "$.ledgerAccountId";
    private static final String ACCOUNT_ID = "$.accountId";
    private static final String CURRENCY = "$.currency";
    private static final String LENGTH = "$.length()";
    private static final String HEADER = "$.header";
    private static final String VALIDATION_ERROR = "VALIDATION ERROR";

    @Test
    void shouldCreateWalletAccount() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post(LEDGER_ACCOUNTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createLedgerAccountRequest(accountId, Currency.GBP))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(LEDGER_ACCOUNT_ID)
                        .value(LedgerAccountIds.deriveWalletAccountId(accountId, Currency.GBP).toString()))
                .andExpect(jsonPath(ACCOUNT_ID).value(accountId.toString()))
                .andExpect(jsonPath("$.accountType").value("WALLET"))
                .andExpect(jsonPath(CURRENCY).value(Currency.GBP.name()))
                .andExpect(jsonPath("$.availableBalance").value(0));
    }

    @Test
    void shouldReturnTheSameWalletWhenCreatedTwice() throws Exception {
        UUID accountId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(createLedgerAccountRequest(accountId, Currency.GBP));

        mockMvc.perform(post(LEDGER_ACCOUNTS_URL).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post(LEDGER_ACCOUNTS_URL).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(LEDGER_ACCOUNT_ID)
                        .value(LedgerAccountIds.deriveWalletAccountId(accountId, Currency.GBP).toString()));
    }

    @Test
    void shouldCreateWalletAccountsInBatch() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post(BATCH_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createLedgerAccountsRequest(accountId, Currency.GBP, Currency.EUR, Currency.JPY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(LENGTH).value(3));
    }

    @Test
    void shouldRetrieveWalletById() throws Exception {
        UUID accountId = UUID.randomUUID();
        givenWallet(accountId, Currency.USD);

        mockMvc.perform(get(BY_ID_URL, LedgerAccountIds.deriveWalletAccountId(accountId, Currency.USD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ACCOUNT_ID).value(accountId.toString()))
                .andExpect(jsonPath(CURRENCY).value(Currency.USD.name()));
    }

    @Test
    void shouldRetrieveEveryWalletHeldForABankAccount() throws Exception {
        UUID accountId = UUID.randomUUID();

        mockMvc.perform(post(BATCH_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createLedgerAccountsRequest(accountId, Currency.GBP, Currency.EUR))))
                .andExpect(status().isCreated());

        mockMvc.perform(get(BY_BANK_ACCOUNT_URL, accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LENGTH).value(2));
    }

    @Test
    void shouldRetrieveWalletByBankAccountAndCurrency() throws Exception {
        UUID accountId = UUID.randomUUID();
        givenWallet(accountId, Currency.JPY);

        mockMvc.perform(get(BY_BANK_ACCOUNT_AND_CURRENCY_URL, accountId, Currency.JPY.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CURRENCY).value(Currency.JPY.name()));
    }

    @Test
    void shouldReturnNotFoundForUnknownLedgerAccount() throws Exception {
        mockMvc.perform(get(BY_ID_URL, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(HEADER).value("NOT EXIST"))
                .andExpect(jsonPath("$.isSuccess").value(false));
    }

    @Test
    void shouldRejectRequestWithoutCurrency() throws Exception {
        mockMvc.perform(post(LEDGER_ACCOUNTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content("{\"accountId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(HEADER).value(VALIDATION_ERROR))
                .andExpect(jsonPath("$.subErrors[0].field").value("currency"));
    }

    @Test
    void shouldRejectBatchRequestWithoutAccounts() throws Exception {
        mockMvc.perform(post(BATCH_URL)
                        .contentType(APPLICATION_JSON)
                        .content("{\"accounts\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(HEADER).value(VALIDATION_ERROR));
    }

    private void givenWallet(UUID accountId, Currency currency) throws Exception {
        mockMvc.perform(post(LEDGER_ACCOUNTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createLedgerAccountRequest(accountId, currency))))
                .andExpect(status().isCreated());
    }
}
