package org.banksolution.integration.account.dto;

import lombok.*;

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
    private String currency;
    private String walletStatus;
    private BigDecimal balance;
    private boolean primary;

}
