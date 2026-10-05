package org.banksolution.integration.account.model;

import java.math.BigDecimal;

public record AccountWalletResponse(
        String currency,
        BigDecimal balance,
        BigDecimal availableBalance) {
}
