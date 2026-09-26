package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.BankLocation;
import org.banksolution.enums.Currency;

import java.util.List;
import java.util.UUID;

@Schema(description = "Details for opening a new account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAccountRequest {

    @Schema(description = "Owning customer's id", example = "3f2a7c1e-8b4d-4e6a-9c2f-1d5e7b9a0c34")
    @NotNull(message = "Customer ID can't be null.")
    private UUID customerId;

    @Schema(description = "Kind of account", example = "CHECKING")
    @NotNull(message = "Account type can't be null.")
    private AccountType accountType;

    @Schema(description = "Country of the holding branch", example = "GB")
    @NotNull(message = "Bank location can't be null.")
    private BankLocation bankLocation;

    @ArraySchema(
            arraySchema = @Schema(description = "Wallet currencies; the first becomes primary"),
            schema = @Schema(example = "GBP")
    )
    @NotEmpty(message = "At least one currency must be specified.")
    private List<Currency> currencies;

}

