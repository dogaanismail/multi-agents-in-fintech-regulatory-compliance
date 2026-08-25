package org.banksolution.controller;

import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.enums.Currency;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.model.response.AccountWalletResponse;
import org.banksolution.service.AccountService;
import org.banksolution.service.AccountWalletService;
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
    private final AccountWalletService accountWalletService;

    @PostMapping("open-account")
    public ResponseEntity<@NonNull AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest openAccountRequest) {
        log.info("POST /api/v1/accounts - Opening account for customer: {}", openAccountRequest.getCustomerId());
        AccountResponse accountResponse = accountService.openAccount(openAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<@NonNull AccountResponse> getAccountById(@PathVariable("id") UUID accountId) {
        log.info("GET /api/v1/accounts/{} - Fetching account", accountId);
        AccountResponse accountResponse = accountService.getAccountById(accountId);
        return ResponseEntity.ok(accountResponse);
    }

    @GetMapping("/ids")
    public ResponseEntity<@NonNull List<AccountResponse>> getByAccountIds(@RequestParam("ids") List<UUID> accountIds) {
        log.info("GET /api/v1/accounts - Fetching accounts with ids: {}", accountIds);
        List<AccountResponse> accountResponses = accountService.getByAccountIds(accountIds);
        return ResponseEntity.ok(accountResponses);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<@NonNull List<AccountResponse>> getAccountsByCustomerId(@PathVariable UUID customerId) {
        log.info("GET /api/v1/accounts/customer/{} - Fetching accounts for customer", customerId);
        List<AccountResponse> accountResponses = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accountResponses);
    }

    @GetMapping("/{id}/wallets")
    public ResponseEntity<@NonNull List<AccountWalletResponse>> getWalletsByAccountId(@PathVariable("id") UUID accountId) {
        log.info("GET /api/v1/accounts/{}/wallets - Fetching wallets", accountId);
        List<AccountWalletResponse> accountWalletResponses = accountWalletService.getWalletsByAccountId(accountId);
        return ResponseEntity.ok(accountWalletResponses);
    }

    @GetMapping("/{id}/wallets/{currency}")
    public ResponseEntity<@NonNull AccountWalletResponse> getWalletByCurrency(
            @PathVariable("id") UUID accountId,
            @PathVariable Currency currency) {

        log.info("GET /api/v1/accounts/{}/wallets/{} - Fetching wallet", accountId, currency);
        AccountWalletResponse accountWalletResponse = accountWalletService.getWalletByCurrency(accountId, currency);
        return ResponseEntity.ok(accountWalletResponse);
    }
}
