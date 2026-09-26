package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.Currency;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Wallet ledger account to open for a bank account")
public class CreateLedgerAccountRequest {

    @Schema(description = "Bank account the wallet belongs to", example = "7c2e9f4a-3b1d-4c8e-9a6f-5d0b2e1c7a83")
    @NotNull(message = "Account ID can't be null.")
    private UUID accountId;

    @Schema(description = "Wallet currency (ISO 4217)", example = "GBP")
    @NotNull(message = "Currency can't be null.")
    private Currency currency;

}
