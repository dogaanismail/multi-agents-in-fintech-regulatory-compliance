package org.banksolution.service;

import org.banksolution.enums.Currency;
import org.banksolution.exception.WalletCreationFailedException;
import org.banksolution.integration.ledger.LedgerServiceClient;
import org.banksolution.integration.ledger.dto.CreateLedgerAccountsRequest;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.banksolution.fixtures.AccountFixtures.createLedgerAccountResponses;
import static org.banksolution.service.CustomerClientServiceTest.createNotFoundFeignException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerClientServiceTest {

    @Mock
    private LedgerServiceClient ledgerServiceClient;

    @InjectMocks
    private LedgerClientService ledgerClientService;

    @Test
    void shouldAskTheLedgerForOneAccountPerCurrency() {
        UUID accountId = UUID.randomUUID();
        List<Currency> currencies = List.of(Currency.GBP, Currency.JPY);
        List<LedgerAccountResponse> ledgerAccountResponses = createLedgerAccountResponses(accountId, currencies);
        when(ledgerServiceClient.createLedgerAccounts(any())).thenReturn(ledgerAccountResponses);

        List<LedgerAccountResponse> openedLedgerAccountResponses = ledgerClientService.openLedgerWallets(accountId, currencies);

        ArgumentCaptor<CreateLedgerAccountsRequest> createLedgerAccountsRequestCaptor =
                ArgumentCaptor.forClass(CreateLedgerAccountsRequest.class);
        verify(ledgerServiceClient).createLedgerAccounts(createLedgerAccountsRequestCaptor.capture());
        assertThat(createLedgerAccountsRequestCaptor.getValue().getAccounts())
                .extracting(
                        CreateLedgerAccountsRequest.CreateLedgerAccountRequest::getAccountId,
                        CreateLedgerAccountsRequest.CreateLedgerAccountRequest::getCurrency)
                .containsExactly(tuple(accountId, Currency.GBP), tuple(accountId, Currency.JPY));
        assertThat(openedLedgerAccountResponses).isSameAs(ledgerAccountResponses);
    }

    @Test
    void shouldWrapALedgerFailureSoTheAccountOpeningRollsBack() {
        UUID accountId = UUID.randomUUID();
        List<Currency> currencies = List.of(Currency.GBP);
        when(ledgerServiceClient.createLedgerAccounts(any())).thenThrow(createNotFoundFeignException());

        assertThatThrownBy(() -> ledgerClientService.openLedgerWallets(accountId, currencies))
                .isInstanceOf(WalletCreationFailedException.class)
                .hasMessageContaining(accountId.toString())
                .hasCauseInstanceOf(feign.FeignException.class);
    }
}
