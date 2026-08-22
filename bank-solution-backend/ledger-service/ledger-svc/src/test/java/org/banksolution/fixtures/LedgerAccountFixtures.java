package org.banksolution.fixtures;

import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.model.request.CreateLedgerAccountRequest;
import org.banksolution.model.request.CreateLedgerAccountsRequest;
import org.banksolution.model.request.CreateLedgerInternalAccountRequest;

import java.util.UUID;
import java.util.stream.Stream;

public final class LedgerAccountFixtures {

    private LedgerAccountFixtures() {
    }

    public static LedgerAccount createWallet(
            UUID accountId,
            Currency currency) {

        return LedgerAccount.newWallet(accountId, currency);
    }

    public static LedgerInternalAccount createInternalAccount(
            LedgerAccountType accountType,
            Currency currency) {

        return LedgerInternalAccount.newInternalAccount(accountType, currency);
    }

    public static CreateLedgerAccountRequest createLedgerAccountRequest(
            UUID accountId,
            Currency currency) {

        return CreateLedgerAccountRequest.builder()
                .accountId(accountId)
                .currency(currency)
                .build();
    }

    public static CreateLedgerAccountsRequest createLedgerAccountsRequest(
            UUID accountId,
            Currency... currencies) {

        return CreateLedgerAccountsRequest.builder()
                .accounts(Stream.of(currencies)
                        .map(currency -> createLedgerAccountRequest(accountId, currency))
                        .toList())
                .build();
    }

    public static CreateLedgerInternalAccountRequest createInternalAccountRequest(
            LedgerAccountType accountType,
            Currency currency) {

        return CreateLedgerInternalAccountRequest.builder()
                .accountType(accountType)
                .currency(currency)
                .build();
    }
}
