package org.banksolution.controller;

import org.banksolution.servicesecurity.RequiresPermission;
import org.banksolution.servicesecurity.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Accounts")
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final AccountWalletService accountWalletService;

    @Operation(summary = "Open an account")
    @PostMapping("open-account")
    @RequiresPermission(Permissions.ACCOUNT_OPEN)
    public ResponseEntity<@NonNull AccountResponse> openAccount(@Valid @RequestBody OpenAccountRequest openAccountRequest) {
        log.info("POST /api/v1/accounts - Opening account for customer: {}", openAccountRequest.getCustomerId());
        AccountResponse accountResponse = accountService.openAccount(openAccountRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountResponse);
    }

    @Operation(summary = "Get an account by id")
    @GetMapping("/{id}")
    @RequiresPermission(Permissions.ACCOUNT_READ)
    public ResponseEntity<@NonNull AccountResponse> getAccountById(@PathVariable("id") UUID accountId) {
        log.info("GET /api/v1/accounts/{} - Fetching account", accountId);
        AccountResponse accountResponse = accountService.getAccountById(accountId);
        return ResponseEntity.ok(accountResponse);
    }

    @Operation(summary = "Get accounts by ids")
    @GetMapping("/ids")
    @RequiresPermission(Permissions.ACCOUNT_READ)
    public ResponseEntity<@NonNull List<AccountResponse>> getByAccountIds(@RequestParam("ids") List<UUID> accountIds) {
        log.info("GET /api/v1/accounts - Fetching accounts with ids: {}", accountIds);
        List<AccountResponse> accountResponses = accountService.getByAccountIds(accountIds);
        return ResponseEntity.ok(accountResponses);
    }

    @Operation(summary = "List a customer's accounts")
    @GetMapping("/customer/{customerId}")
    @RequiresPermission(Permissions.ACCOUNT_READ)
    public ResponseEntity<@NonNull List<AccountResponse>> getAccountsByCustomerId(@PathVariable UUID customerId) {
        log.info("GET /api/v1/accounts/customer/{} - Fetching accounts for customer", customerId);
        List<AccountResponse> accountResponses = accountService.getAccountsByCustomerId(customerId);
        return ResponseEntity.ok(accountResponses);
    }

    @Operation(summary = "List an account's wallets")
    @GetMapping("/{id}/wallets")
    @RequiresPermission(Permissions.ACCOUNT_READ)
    public ResponseEntity<@NonNull List<AccountWalletResponse>> getWalletsByAccountId(@PathVariable("id") UUID accountId) {
        log.info("GET /api/v1/accounts/{}/wallets - Fetching wallets", accountId);
        List<AccountWalletResponse> accountWalletResponses = accountWalletService.getWalletsByAccountId(accountId);
        return ResponseEntity.ok(accountWalletResponses);
    }

    @Operation(summary = "Get an account wallet by currency")
    @GetMapping("/{id}/wallets/{currency}")
    @RequiresPermission(Permissions.ACCOUNT_READ)
    public ResponseEntity<@NonNull AccountWalletResponse> getWalletByCurrency(
            @PathVariable("id") UUID accountId,
            @PathVariable Currency currency) {

        log.info("GET /api/v1/accounts/{}/wallets/{} - Fetching wallet", accountId, currency);
        AccountWalletResponse accountWalletResponse = accountWalletService.getWalletByCurrency(accountId, currency);
        return ResponseEntity.ok(accountWalletResponse);
    }
}
