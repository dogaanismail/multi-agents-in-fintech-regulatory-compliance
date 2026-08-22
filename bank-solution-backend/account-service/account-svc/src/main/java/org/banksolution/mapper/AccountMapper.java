package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;

import java.time.LocalDate;
import java.util.List;

@UtilityClass
public class AccountMapper {

    public static AccountEntity toAccountEntity(
            OpenAccountRequest request,
            String accountNumber) {

        return AccountEntity.builder()
                .customerId(request.getCustomerId())
                .accountNumber(accountNumber)
                .bankLocation(request.getBankLocation())
                .accountType(request.getAccountType())
                .openingDate(LocalDate.now())
                .build();
    }

    public static AccountResponse toAccountResponse(
            AccountEntity account,
            List<AccountWalletEntity> wallets) {

        return AccountResponse.builder()
                .id(account.getId())
                .customerId(account.getCustomerId())
                .accountNumber(account.getAccountNumber())
                .bankLocation(account.getBankLocation().name().toUpperCase())
                .accountType(account.getAccountType())
                .accountStatus(account.getAccountStatus())
                .openingDate(account.getOpeningDate())
                .closingDate(account.getClosingDate())
                .wallets(wallets.stream()
                        .map(AccountWalletMapper::toAccountWalletResponse)
                        .toList())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
