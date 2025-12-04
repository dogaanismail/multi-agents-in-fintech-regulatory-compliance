package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.model.response.BalanceResponse;

@UtilityClass
public class BalanceMapper {

    public static BalanceResponse toResponse(
            AccountBalanceEntity entity) {

        return BalanceResponse.builder()
                .id(entity.getId())
                .currency(entity.getCurrency())
                .availableBalance(entity.getAvailableBalance())
                .pendingBalance(entity.getPendingBalance())
                .totalBalance(entity.getTotalBalance())
                .build();
    }
}
