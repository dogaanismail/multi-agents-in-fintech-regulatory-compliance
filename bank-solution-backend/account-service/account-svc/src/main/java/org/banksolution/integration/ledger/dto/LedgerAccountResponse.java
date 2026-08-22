package org.banksolution.integration.ledger.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.banksolution.enums.Currency;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerAccountResponse {

    private UUID ledgerAccountId;
    private UUID accountId;
    private Currency currency;

}
