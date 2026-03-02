package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.model.response.ExchangeRateResponse;

@UtilityClass
public class ExchangeRateMapper {

    public static ExchangeRateResponse toResponse(ExchangeRateEntity entity) {
        return ExchangeRateResponse.builder()
                .currencyPair(entity.getCurrencyPair())
                .rate(entity.getRate())
                .fetchedAt(entity.getFetchedAt())
                .build();
    }
}
