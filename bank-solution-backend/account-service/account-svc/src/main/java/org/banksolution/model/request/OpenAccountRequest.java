package org.banksolution.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.Currency;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAccountRequest {

    @NotNull(message = "Customer ID can't be null.")
    private UUID customerId;

    @NotNull(message = "Account type can't be null.")
    private AccountType accountType;

    @NotNull(message = "Bank location can't be null.")
    private String bankLocation;  //TODO: Country name or country 2 letter code?

    @NotNull(message = "At least one currency must be specified.")
    private List<Currency> currencies;

}

