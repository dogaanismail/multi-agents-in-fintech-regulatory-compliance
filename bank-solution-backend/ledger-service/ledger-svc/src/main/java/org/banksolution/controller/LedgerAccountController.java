package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.enums.Currency;
import org.banksolution.mapper.LedgerAccountMapper;
import org.banksolution.model.request.CreateLedgerAccountRequest;
import org.banksolution.model.request.CreateLedgerAccountsRequest;
import org.banksolution.model.response.LedgerAccountResponse;
import org.banksolution.service.LedgerAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger/accounts")
@Tag(name = "Ledger Accounts")
@RequiredArgsConstructor
@Slf4j
public class LedgerAccountController {

    private final LedgerAccountService ledgerAccountService;

    @PostMapping
    @Operation(summary = "Create a ledger account", description = "Creates a wallet ledger account for a bank account and currency")
    public ResponseEntity<@NonNull LedgerAccountResponse> createLedgerAccount(
            @Valid @RequestBody CreateLedgerAccountRequest request) {
        log.info("POST /api/v1/ledger/accounts - accountId: {}, currency: {}",
                request.getAccountId(), request.getCurrency());

        LedgerAccount ledgerAccount = ledgerAccountService
                .createLedgerAccount(request.getAccountId(), request.getCurrency());

        return ResponseEntity.status(HttpStatus.CREATED).body(LedgerAccountMapper.toLedgerAccountResponse(ledgerAccount));
    }

    @PostMapping("/batch")
    @Operation(summary = "Create ledger accounts", description = "Creates several wallet ledger accounts in one batch")
    public ResponseEntity<@NonNull List<LedgerAccountResponse>> createLedgerAccounts(
            @Valid @RequestBody CreateLedgerAccountsRequest request) {
        log.info("POST /api/v1/ledger/accounts/batch - {} accounts", request.getAccounts().size());

        List<LedgerAccount> ledgerAccounts = ledgerAccountService.createLedgerAccounts(
                request.getAccounts().stream()
                        .map(account -> LedgerAccount.newWallet(account.getAccountId(), account.getCurrency()))
                        .toList());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ledgerAccounts.stream().map(LedgerAccountMapper::toLedgerAccountResponse).toList());
    }

    @GetMapping("/{ledgerAccountId}")
    @Operation(summary = "Retrieve a ledger account", description = "Retrieves a ledger account and its balances")
    public ResponseEntity<@NonNull LedgerAccountResponse> getLedgerAccount(@PathVariable UUID ledgerAccountId) {
        log.info("GET /api/v1/ledger/accounts/{}", ledgerAccountId);
        return ResponseEntity.ok(LedgerAccountMapper.toLedgerAccountResponse(ledgerAccountService.getLedgerAccount(ledgerAccountId)));
    }

    @GetMapping("/bank-account/{accountId}")
    @Operation(summary = "Retrieve wallets for a bank account", description = "Retrieves every currency wallet held for a bank account")
    public ResponseEntity<@NonNull List<LedgerAccountResponse>> getWallets(@PathVariable UUID accountId) {
        log.info("GET /api/v1/ledger/accounts/bank-account/{}", accountId);
        return ResponseEntity.ok(ledgerAccountService.getWallets(accountId).stream()
                .map(LedgerAccountMapper::toLedgerAccountResponse)
                .toList());
    }

    @GetMapping("/bank-account/{accountId}/{currency}")
    @Operation(summary = "Retrieve a wallet", description = "Retrieves the wallet held for a bank account in one currency")
    public ResponseEntity<@NonNull LedgerAccountResponse> getWallet(
            @PathVariable UUID accountId,
            @PathVariable Currency currency) {
        log.info("GET /api/v1/ledger/accounts/bank-account/{}/{}", accountId, currency);
        return ResponseEntity.ok(LedgerAccountMapper.toLedgerAccountResponse(ledgerAccountService.getWallet(accountId, currency)));
    }
}
