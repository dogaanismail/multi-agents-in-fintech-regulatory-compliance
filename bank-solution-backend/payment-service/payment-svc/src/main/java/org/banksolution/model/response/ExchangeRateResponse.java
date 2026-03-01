package org.banksolution.model.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateResponse {

    private String currencyPair;
    private BigDecimal rate;
    private Instant fetchedAt;
}
