package org.banksolution.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.model.response.ExchangeRateResponse;
import org.banksolution.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<List<ExchangeRateResponse>> getAllRates() {
        log.debug("GET /api/v1/exchange-rates - Fetching all exchange rates");
        return ResponseEntity.ok(exchangeRateService.getAllRates());
    }

    @GetMapping("/{from}/{to}")
    public ResponseEntity<ExchangeRateResponse> getRate(
            @PathVariable("from") String fromCurrency,
            @PathVariable("to") String toCurrency) {
        log.info("GET /api/v1/exchange-rates/{}/{} - Fetching exchange rate", fromCurrency, toCurrency);
        return exchangeRateService.getRate(fromCurrency.toUpperCase(), toCurrency.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
