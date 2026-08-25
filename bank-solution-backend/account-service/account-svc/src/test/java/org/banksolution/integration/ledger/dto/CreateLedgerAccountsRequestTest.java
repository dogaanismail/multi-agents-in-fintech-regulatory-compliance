package org.banksolution.integration.ledger.dto;

import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CreateLedgerAccountsRequestTest {

    @Test
    void shouldRequestOneLedgerAccountPerCurrencyForTheSameAccount() {
        UUID accountId = UUID.randomUUID();

        CreateLedgerAccountsRequest createLedgerAccountsRequest =
                CreateLedgerAccountsRequest.forCurrencies(accountId, List.of(Currency.GBP, Currency.JPY));

        assertThat(createLedgerAccountsRequest.getAccounts())
                .extracting(
                        CreateLedgerAccountsRequest.CreateLedgerAccountRequest::getAccountId,
                        CreateLedgerAccountsRequest.CreateLedgerAccountRequest::getCurrency)
                .containsExactly(tuple(accountId, Currency.GBP), tuple(accountId, Currency.JPY));
    }
}
