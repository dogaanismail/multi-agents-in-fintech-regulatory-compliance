package org.banksolution.model.response;

import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerInternalAccountResponse {

    private UUID ledgerAccountId;
    private LedgerAccountType accountType;
    private Currency currency;
    private BigDecimal creditsPosted;
    private BigDecimal creditsPending;
    private BigDecimal debitsPosted;
    private BigDecimal debitsPending;
    private BigDecimal netBalance;
    private Instant createdAt;

}
