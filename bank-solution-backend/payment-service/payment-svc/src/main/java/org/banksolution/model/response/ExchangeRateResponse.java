package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Stored exchange rate for a currency pair")
public class ExchangeRateResponse {

    @Schema(description = "Source and target currency codes", example = "EURUSD")
    private String currencyPair;

    @Schema(description = "Units of target per source unit", example = "1.0850")
    private BigDecimal rate;

    @Schema(description = "When the rate was fetched", example = "2026-09-27T10:15:30Z")
    private Instant fetchedAt;
}
