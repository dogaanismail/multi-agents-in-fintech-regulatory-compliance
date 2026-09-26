package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Internal ledger account to open")
public class CreateLedgerInternalAccountRequest {

    @Schema(description = "Internal account type", example = "INBOUND_CLEARING")
    @NotNull(message = "Account type can't be null.")
    private LedgerAccountType accountType;

    @Schema(description = "Account currency (ISO 4217)", example = "GBP")
    @NotNull(message = "Currency can't be null.")
    private Currency currency;

}
