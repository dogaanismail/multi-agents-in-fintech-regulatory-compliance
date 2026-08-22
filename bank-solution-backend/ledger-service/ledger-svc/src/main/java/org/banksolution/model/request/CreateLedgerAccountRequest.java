package org.banksolution.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.Currency;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLedgerAccountRequest {

    @NotNull(message = "Account ID can't be null.")
    private UUID accountId;

    @NotNull(message = "Currency can't be null.")
    private Currency currency;

}
