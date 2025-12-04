package org.banksolution.model.response;

import lombok.*;
import org.banksolution.entity.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private UUID id;
    private Currency currency;
    private BigDecimal availableBalance;
    private BigDecimal pendingBalance;
    private BigDecimal totalBalance;

}
