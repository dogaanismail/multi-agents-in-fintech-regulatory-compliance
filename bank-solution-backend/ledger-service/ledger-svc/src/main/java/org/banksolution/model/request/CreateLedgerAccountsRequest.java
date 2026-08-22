package org.banksolution.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLedgerAccountsRequest {

    @Valid
    @NotEmpty(message = "At least one ledger account must be specified.")
    private List<CreateLedgerAccountRequest> accounts;

}
