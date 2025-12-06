package org.banksolution.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.enums.Currency;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.model.response.BalanceResponse;
import org.banksolution.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<@NonNull AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest request) {
        log.info("POST /api/v1/accounts - Opening account for customer: {}", request.getCustomerId());
        AccountResponse response = accountService.openAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull AccountResponse> getAccountById(@PathVariable UUID id) {
        log.info("GET /api/v1/accounts/{} - Fetching account", id);
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull List<AccountResponse>> getAccountsByCustomerId(@PathVariable UUID customerId) {
        log.info("GET /api/v1/accounts/customer/{} - Fetching accounts for customer", customerId);
        List<AccountResponse> response = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balances")
    public ResponseEntity<@NonNull List<BalanceResponse>> getBalancesByAccountId(@PathVariable UUID id) {
        log.info("GET /api/v1/accounts/{}/balances - Fetching balances", id);
        List<BalanceResponse> response = accountService.getBalancesByAccountId(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/balances/{currency}")
    public ResponseEntity<@NonNull BalanceResponse> getBalanceByCurrency(
            @PathVariable UUID id,
            @PathVariable Currency currency) {
        log.info("GET /api/v1/accounts/{}/balances/{} - Fetching balance", id, currency);
        BalanceResponse response = accountService.getBalanceByCurrency(id, currency);
        return ResponseEntity.ok(response);
    }
}

