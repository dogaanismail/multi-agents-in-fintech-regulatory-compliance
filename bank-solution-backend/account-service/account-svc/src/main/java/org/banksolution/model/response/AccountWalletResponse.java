package org.banksolution.model.response;

import lombok.*;
import org.banksolution.enums.Currency;
import org.banksolution.enums.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountWalletResponse {

    private UUID id;
    private UUID ledgerAccountId;
    private Currency currency;
    private WalletStatus walletStatus;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private boolean primary;

}
