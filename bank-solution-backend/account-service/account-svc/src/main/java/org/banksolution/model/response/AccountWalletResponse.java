package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Single-currency wallet within an account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountWalletResponse {

    @Schema(description = "Wallet id", example = "c4e8a2f1-3b7d-4f9a-8e6c-5d1b0a2f7e93")
    private UUID id;

    @Schema(description = "Backing ledger account id", example = "9d3f6b8e-1a2c-4e5f-a7b9-0c4d8e2f6a15")
    private UUID ledgerAccountId;

    @Schema(description = "Wallet currency", example = "GBP")
    private Currency currency;

    @Schema(description = "Wallet's lifecycle status", example = "ACTIVE")
    private WalletStatus walletStatus;

    @Schema(description = "Ledger balance", example = "1250.00")
    private BigDecimal balance;

    @Schema(description = "Balance available to spend", example = "1100.00")
    private BigDecimal availableBalance;

    @Schema(description = "Whether this is the account's primary wallet")
    private boolean primary;

}
