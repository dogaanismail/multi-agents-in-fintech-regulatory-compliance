package org.banksolution.service;

import org.banksolution.integration.account.AccountServiceClient;
import org.banksolution.integration.account.dto.AccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.IntegrationClientFixtures.createAccountResponse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldReturnTheAccountFetchedById() {
        UUID accountId = UUID.randomUUID();
        AccountResponse accountResponse = createAccountResponse(accountId, "GB000111", "GB");
        when(accountServiceClient.getAccountById(accountId)).thenReturn(accountResponse);

        assertThat(accountService.getAccountById(accountId)).isEqualTo(accountResponse);
    }

    @Test
    void shouldReturnTheAccountsFetchedByIds() {
        UUID accountId = UUID.randomUUID();
        List<AccountResponse> accountResponses = List.of(createAccountResponse(accountId, "GB000111", "GB"));
        when(accountServiceClient.getAccountsByIds(List.of(accountId))).thenReturn(accountResponses);

        assertThat(accountService.getAccountsByIds(List.of(accountId))).isEqualTo(accountResponses);
    }
}
