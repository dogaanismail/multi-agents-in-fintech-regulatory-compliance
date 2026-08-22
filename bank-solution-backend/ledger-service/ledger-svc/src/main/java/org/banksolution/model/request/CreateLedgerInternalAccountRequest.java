package org.banksolution.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLedgerInternalAccountRequest {

    @NotNull(message = "Account type can't be null.")
    private LedgerAccountType accountType;

    @NotNull(message = "Currency can't be null.")
    private Currency currency;

}
