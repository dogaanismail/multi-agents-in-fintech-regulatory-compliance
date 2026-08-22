package org.banksolution.model.response;

import lombok.*;
import org.banksolution.enums.Currency;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrialBalanceResponse {

    private Currency currency;
    private BigDecimal net;
    private boolean balanced;
    private List<LedgerInternalAccountResponse> internalAccounts;

}
