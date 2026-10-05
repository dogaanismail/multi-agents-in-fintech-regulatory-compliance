package org.banksolution.model.response;

import java.math.BigDecimal;

public record MobileWalletResponse(
        String currency,
        BigDecimal balance,
        BigDecimal availableBalance) {
}
