package org.banksolution.mapper;

import org.banksolution.integration.account.model.AccountResponse;
import org.banksolution.integration.account.model.AccountWalletResponse;
import org.banksolution.integration.account.model.OpenAccountRequest;
import org.banksolution.model.request.MobileOpenAccountRequest;
import org.banksolution.model.response.MobileAccountResponse;
import org.banksolution.model.response.MobileWalletResponse;

import java.util.List;
import java.util.UUID;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static OpenAccountRequest toOpenAccountRequest(
            UUID customerId,
            MobileOpenAccountRequest mobileOpenAccountRequest) {

        return new OpenAccountRequest(
                customerId,
                mobileOpenAccountRequest.accountType(),
                mobileOpenAccountRequest.bankLocation(),
                mobileOpenAccountRequest.currencies());
    }

    public static MobileAccountResponse toMobileAccountResponse(AccountResponse accountResponse) {
        List<AccountWalletResponse> wallets = accountResponse.wallets() == null ? List.of() : accountResponse.wallets();
        return new MobileAccountResponse(
                accountResponse.id(),
                accountResponse.accountNumber(),
                accountResponse.accountType(),
                accountResponse.accountStatus(),
                wallets.stream().map(AccountMapper::toMobileWalletResponse).toList());
    }

    private static MobileWalletResponse toMobileWalletResponse(AccountWalletResponse accountWalletResponse) {
        return new MobileWalletResponse(
                accountWalletResponse.currency(),
                accountWalletResponse.balance(),
                accountWalletResponse.availableBalance());
    }
}
