package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Internal ledger account with its balances")
public class LedgerInternalAccountResponse {

    @Schema(description = "Ledger account identifier", example = "4d6b9e1f-8c2a-4b3d-a7e5-0f9c2b8d1a64")
    private UUID ledgerAccountId;

    @Schema(description = "Internal account type", example = "INBOUND_CLEARING")
    private LedgerAccountType accountType;

    @Schema(description = "Account currency (ISO 4217)", example = "GBP")
    private Currency currency;

    @Schema(description = "Settled credits in major units", example = "1500.00")
    private BigDecimal creditsPosted;

    @Schema(description = "Reserved credits in major units", example = "0.00")
    private BigDecimal creditsPending;

    @Schema(description = "Settled debits in major units", example = "250.00")
    private BigDecimal debitsPosted;

    @Schema(description = "Reserved debits in major units", example = "100.00")
    private BigDecimal debitsPending;

    @Schema(description = "Net balance in major units", example = "-1150.00")
    private BigDecimal netBalance;

    @Schema(description = "When the account was created", example = "2026-09-27T10:15:30Z")
    private Instant createdAt;

}
