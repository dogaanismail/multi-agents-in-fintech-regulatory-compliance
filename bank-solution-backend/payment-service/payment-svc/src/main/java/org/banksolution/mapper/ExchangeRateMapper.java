package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.ExchangeRateEntity;
import org.banksolution.model.response.ExchangeRateResponse;

@UtilityClass
public class ExchangeRateMapper {

    public static ExchangeRateResponse toExchangeRateResponse(ExchangeRateEntity exchangeRateEntity) {
        return ExchangeRateResponse.builder()
                .currencyPair(exchangeRateEntity.getCurrencyPair())
                .rate(exchangeRateEntity.getRate())
                .fetchedAt(exchangeRateEntity.getFetchedAt())
                .build();
    }
}
