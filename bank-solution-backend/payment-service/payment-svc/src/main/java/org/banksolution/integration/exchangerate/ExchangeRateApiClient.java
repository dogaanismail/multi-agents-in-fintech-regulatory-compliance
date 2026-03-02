package org.banksolution.integration.exchangerate;

import org.banksolution.integration.exchangerate.dto.ExchangeRateApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "exchange-rate-api",
        url = "${integration.exchange-rate-api.base-url}"
)
public interface ExchangeRateApiClient {

    @GetMapping("/{baseCurrency}")
    ExchangeRateApiResponse fetchRates(@PathVariable String baseCurrency);
}
