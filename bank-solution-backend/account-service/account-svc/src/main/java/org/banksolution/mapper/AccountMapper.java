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
            OpenAccountRequest openAccountRequest,
            String accountNumber,
            LocalDate openingDate) {

        return AccountEntity.builder()
                .customerId(openAccountRequest.getCustomerId())
                .accountNumber(accountNumber)
                .bankLocation(openAccountRequest.getBankLocation())
                .accountType(openAccountRequest.getAccountType())
                .openingDate(openingDate)
                .build();
    }

    public static AccountResponse toAccountResponse(
            AccountEntity accountEntity,
            List<AccountWalletEntity> accountWalletEntities) {

        return AccountResponse.builder()
                .id(accountEntity.getId())
                .customerId(accountEntity.getCustomerId())
                .accountNumber(accountEntity.getAccountNumber())
                .bankLocation(accountEntity.getBankLocation().name().toUpperCase())
                .accountType(accountEntity.getAccountType())
                .accountStatus(accountEntity.getAccountStatus())
                .openingDate(accountEntity.getOpeningDate())
                .closingDate(accountEntity.getClosingDate())
                .wallets(accountWalletEntities.stream()
                        .map(AccountWalletMapper::toAccountWalletResponse)
                        .toList())
                .createdAt(accountEntity.getCreatedAt())
                .updatedAt(accountEntity.getUpdatedAt())
                .build();
    }
}
