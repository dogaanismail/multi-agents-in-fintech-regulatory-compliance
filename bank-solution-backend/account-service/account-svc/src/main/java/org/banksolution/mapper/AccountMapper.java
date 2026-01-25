package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.entity.AccountEntity;
import org.banksolution.enums.Currency;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@UtilityClass
public class AccountMapper {

    public static AccountEntity toEntity(
            OpenAccountRequest request,
            String accountNumber) {

        AccountEntity account = AccountEntity.builder()
                .customerId(request.getCustomerId())
                .accountNumber(accountNumber)
                .bankLocation(request.getBankLocation())
                .accountType(request.getAccountType())
                .openingDate(LocalDate.now())
                .build();

        List<AccountBalanceEntity> balances = request.getCurrencies().stream()
                .map(currency -> createBalance(account, currency))
                .toList();
        account.setBalances(balances);

        return account;
    }

    private static AccountBalanceEntity createBalance(
            AccountEntity account,
            Currency currency) {

        return AccountBalanceEntity.builder()
                .account(account)
                .currency(currency)
                .availableBalance(BigDecimal.ZERO)
                .pendingBalance(BigDecimal.ZERO)
                .build();
    }

    public static AccountResponse toResponse(
            AccountEntity entity) {

        return AccountResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .accountNumber(entity.getAccountNumber())
                .bankLocation(entity.getBankLocation().name().toUpperCase())
                .accountType(entity.getAccountType())
                .accountStatus(entity.getAccountStatus())
                .openingDate(entity.getOpeningDate())
                .closingDate(entity.getClosingDate())
                .balances(entity.getBalances().stream()
                        .map(BalanceMapper::toResponse)
                        .toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

}

