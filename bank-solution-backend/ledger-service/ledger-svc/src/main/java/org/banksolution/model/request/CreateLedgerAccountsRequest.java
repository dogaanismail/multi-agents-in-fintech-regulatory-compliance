package org.banksolution.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Batch of wallet ledger accounts to open")
public class CreateLedgerAccountsRequest {

    @Schema(description = "Wallet ledger accounts to create")
    @Valid
    @NotEmpty(message = "At least one ledger account must be specified.")
    private List<CreateLedgerAccountRequest> accounts;

}
