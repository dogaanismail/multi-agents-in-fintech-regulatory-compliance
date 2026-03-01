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
            @PathVariable String from,
            @PathVariable String to) {
        log.info("GET /api/v1/exchange-rates/{}/{} - Fetching exchange rate", from, to);
        return exchangeRateService.getRate(from.toUpperCase(), to.toUpperCase())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
