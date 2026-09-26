package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.banksolution.enums.Currency;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Internal accounts netted against customer wallets")
public class TrialBalanceResponse {

    @Schema(description = "Currency of the trial balance", example = "GBP")
    private Currency currency;

    @Schema(description = "Net of all internal accounts", example = "-1150.00")
    private BigDecimal internalAccountsNet;

    @Schema(description = "Net of all customer wallets", example = "1150.00")
    private BigDecimal customerWalletsNet;

    @Schema(description = "Overall net; zero when balanced", example = "0.00")
    private BigDecimal net;

    @Schema(description = "Whether the book sums to zero")
    private boolean balanced;

    @Schema(description = "Internal accounts included in the total")
    private List<LedgerInternalAccountResponse> internalAccounts;

}
