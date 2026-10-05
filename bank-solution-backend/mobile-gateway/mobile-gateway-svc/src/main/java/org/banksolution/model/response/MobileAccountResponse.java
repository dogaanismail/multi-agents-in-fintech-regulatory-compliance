package org.banksolution.model.response;

import java.util.List;
import java.util.UUID;

public record MobileAccountResponse(
        UUID accountId,
        String accountNumber,
        String accountType,
        String status,
        List<MobileWalletResponse> wallets) {
}
