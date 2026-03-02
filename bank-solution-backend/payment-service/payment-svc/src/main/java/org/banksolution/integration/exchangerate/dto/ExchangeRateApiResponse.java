package org.banksolution.integration.exchangerate.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateApiResponse {

    @JsonProperty("base")
    private String base;

    @JsonProperty("date")
    private String date;

    @JsonProperty("time_last_updated")
    private long timeLastUpdated;

    @JsonProperty("rates")
    private Map<String, BigDecimal> rates;
}
