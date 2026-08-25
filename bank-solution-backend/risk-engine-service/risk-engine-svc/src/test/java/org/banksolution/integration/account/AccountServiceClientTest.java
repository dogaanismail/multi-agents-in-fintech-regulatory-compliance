package org.banksolution.integration.account;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;
import org.banksolution.integration.account.dto.AccountResponse;
import org.banksolution.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.common.initializers.WireMockInitializer.ACCOUNT_SERVICE_BASE_PATH;
import static org.banksolution.fixtures.IntegrationClientFixtures.createAccountResponse;

class AccountServiceClientTest extends BaseIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Test
    void shouldDeserializeASingleAccountIncludingEnumsAndDates() {
        UUID accountId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo(ACCOUNT_SERVICE_BASE_PATH + "/" + accountId))
                .willReturn(okJson(objectMapper.writeValueAsString(
                        createAccountResponse(accountId, "GB000111", "GB")))));

        AccountResponse account = accountService.getAccountById(accountId);

        assertThat(account.getId()).isEqualTo(accountId);
        assertThat(account.getAccountNumber()).isEqualTo("GB000111");
        assertThat(account.getBankLocation()).isEqualTo("GB");
        assertThat(account.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(account.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getOpeningDate()).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void shouldDeserializeTheBatchLookupOfSeveralAccounts() {
        UUID firstAccountId = UUID.randomUUID();
        UUID secondAccountId = UUID.randomUUID();
        stubFor(get(urlPathEqualTo(ACCOUNT_SERVICE_BASE_PATH + "/ids"))
                .willReturn(okJson(objectMapper.writeValueAsString(List.of(
                        createAccountResponse(firstAccountId, "GB000111", "GB"),
                        createAccountResponse(secondAccountId, "DE000222", "DE"))))));

        List<AccountResponse> accounts = accountService.getAccountsByIds(List.of(firstAccountId, secondAccountId));

        assertThat(accounts)
                .extracting(AccountResponse::getId)
                .containsExactly(firstAccountId, secondAccountId);
    }
}
