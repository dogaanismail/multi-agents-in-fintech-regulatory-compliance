package org.banksolution.integration.account.model;

import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        UUID customerId,
        String accountNumber,
        String accountType,
        String accountStatus,
        List<AccountWalletResponse> wallets) {
}
