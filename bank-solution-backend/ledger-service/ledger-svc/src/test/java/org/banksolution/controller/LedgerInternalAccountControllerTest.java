package org.banksolution.controller;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.junit.jupiter.api.Test;

import static org.banksolution.fixtures.LedgerAccountFixtures.createInternalAccountRequest;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LedgerInternalAccountControllerTest extends BaseIntegrationTest {

    private static final String INTERNAL_ACCOUNTS_URL = "/api/v1/ledger/internal-accounts";
    private static final String BY_ID_URL = INTERNAL_ACCOUNTS_URL + "/{ledgerAccountId}";
    private static final String TRIAL_BALANCE_URL = INTERNAL_ACCOUNTS_URL + "/trial-balance/{currency}";

    private static final String ACCOUNT_TYPE = "$.accountType";
    private static final String CURRENCY = "$.currency";
    private static final String LENGTH = "$.length()";
    private static final String HEADER = "$.header";
    private static final String CURRENCY_PARAM = "currency";

    private static final int SEEDED_CURRENCIES = 3;

    @Test
    void shouldSeedTheChartOfAccountsOnStartup() throws Exception {
        int expected = SEEDED_CURRENCIES * LedgerAccountType.internalTypes().length;

        mockMvc.perform(get(INTERNAL_ACCOUNTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LENGTH).value(expected));
    }

    @Test
    void shouldListInternalAccountsForOneCurrency() throws Exception {
        mockMvc.perform(get(INTERNAL_ACCOUNTS_URL).param(CURRENCY_PARAM, Currency.GBP.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LENGTH).value(LedgerAccountType.internalTypes().length))
                .andExpect(jsonPath("$[*].currency", everyItem(equalTo(Currency.GBP.name()))));
    }

    @Test
    void shouldCreateInternalAccountForANewCurrency() throws Exception {
        mockMvc.perform(post(INTERNAL_ACCOUNTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createInternalAccountRequest(LedgerAccountType.FEES_INCOME, Currency.TRY))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ledgerAccountId")
                        .value(LedgerAccountIds.internal(LedgerAccountType.FEES_INCOME, Currency.TRY).toString()))
                .andExpect(jsonPath(ACCOUNT_TYPE).value(LedgerAccountType.FEES_INCOME.name()))
                .andExpect(jsonPath(CURRENCY).value(Currency.TRY.name()))
                .andExpect(jsonPath("$.netBalance").value(0));
    }

    @Test
    void shouldRetrieveInternalAccountById() throws Exception {
        mockMvc.perform(get(BY_ID_URL,
                        LedgerAccountIds.internal(LedgerAccountType.INBOUND_CLEARING, Currency.GBP)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ACCOUNT_TYPE).value(LedgerAccountType.INBOUND_CLEARING.name()))
                .andExpect(jsonPath(CURRENCY).value(Currency.GBP.name()));
    }

    @Test
    void shouldReportABalancedTrialBalance() throws Exception {
        mockMvc.perform(get(TRIAL_BALANCE_URL, Currency.GBP.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(CURRENCY).value(Currency.GBP.name()))
                .andExpect(jsonPath("$.net").value(0))
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.internalAccounts.length()")
                        .value(LedgerAccountType.internalTypes().length));
    }

    @Test
    void shouldRejectWalletAsAnInternalAccountType() throws Exception {
        mockMvc.perform(post(INTERNAL_ACCOUNTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                createInternalAccountRequest(LedgerAccountType.WALLET, Currency.GBP))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(HEADER).value("VALIDATION ERROR"))
                .andExpect(jsonPath("$.message").value("WALLET is not an internal account type"));
    }

    @Test
    void shouldRejectRequestWithoutAccountType() throws Exception {
        mockMvc.perform(post(INTERNAL_ACCOUNTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content("{\"currency\":\"" + Currency.GBP.name() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors[0].field").value("accountType"));
    }
}
