package org.banksolution.integration.ledger.dto;

import lombok.*;
import org.banksolution.enums.Currency;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLedgerAccountsRequest {

    private List<CreateLedgerAccountRequest> accounts;

    public static CreateLedgerAccountsRequest forCurrencies(
            UUID accountId,
            List<Currency> currencies) {

        return CreateLedgerAccountsRequest.builder()
                .accounts(currencies.stream()
                        .map(currency -> CreateLedgerAccountRequest.builder()
                                .accountId(accountId)
                                .currency(currency)
                                .build())
                        .toList())
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateLedgerAccountRequest {

        private UUID accountId;
        private Currency currency;

    }
}
