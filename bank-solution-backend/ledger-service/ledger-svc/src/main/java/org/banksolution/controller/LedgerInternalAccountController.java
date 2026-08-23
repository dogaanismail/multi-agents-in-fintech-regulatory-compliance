package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.mapper.LedgerAccountMapper;
import org.banksolution.model.request.CreateLedgerInternalAccountRequest;
import org.banksolution.model.response.LedgerInternalAccountResponse;
import org.banksolution.model.response.TrialBalanceResponse;
import org.banksolution.service.LedgerAccountService;
import org.banksolution.service.LedgerInternalAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger/internal-accounts")
@Tag(name = "Ledger Internal Accounts")
@RequiredArgsConstructor
@Slf4j
public class LedgerInternalAccountController {

    private final LedgerInternalAccountService ledgerInternalAccountService;
    private final LedgerAccountService ledgerAccountService;

    @PostMapping
    @Operation(summary = "Create an internal ledger account", description = "Creates an internal ledger account for a type and currency")
    public ResponseEntity<@NonNull LedgerInternalAccountResponse> createInternalAccount(
            @Valid @RequestBody CreateLedgerInternalAccountRequest request) {
        log.info("POST /api/v1/ledger/internal-accounts - type: {}, currency: {}",
                request.getAccountType(),
                request.getCurrency());

        LedgerInternalAccount internalAccount = ledgerInternalAccountService
                .createInternalAccount(request.getAccountType(), request.getCurrency());

        return ResponseEntity.status(HttpStatus.CREATED).body(LedgerAccountMapper.toLedgerInternalAccountResponse(internalAccount));
    }

    @GetMapping
    @Operation(summary = "List internal ledger accounts", description = "Lists internal ledger accounts, optionally filtered by currency")
    public ResponseEntity<@NonNull List<LedgerInternalAccountResponse>> getInternalAccounts(
            @RequestParam(required = false) Currency currency) {
        log.info("GET /api/v1/ledger/internal-accounts - currency: {}", currency);

        List<LedgerInternalAccount> internalAccounts = currency == null
                ? ledgerInternalAccountService.getInternalAccounts()
                : ledgerInternalAccountService.getInternalAccounts(currency);

        return ResponseEntity.ok(internalAccounts.stream().map(LedgerAccountMapper::toLedgerInternalAccountResponse).toList());
    }

    @GetMapping("/{ledgerAccountId}")
    @Operation(summary = "Retrieve an internal ledger account", description = "Retrieves an internal ledger account and its balances")
    public ResponseEntity<@NonNull LedgerInternalAccountResponse> getInternalAccount(
            @PathVariable UUID ledgerAccountId) {
        log.info("GET /api/v1/ledger/internal-accounts/{}", ledgerAccountId);
        return ResponseEntity.ok(
                LedgerAccountMapper.toLedgerInternalAccountResponse(ledgerInternalAccountService.getInternalAccount(ledgerAccountId)));
    }

    @GetMapping("/trial-balance/{currency}")
    @Operation(summary = "Trial balance",
            description = "Nets the internal accounts against the customer wallets for a currency; a balanced book sums to zero")
    public ResponseEntity<@NonNull TrialBalanceResponse> getTrialBalance(@PathVariable Currency currency) {
        log.info("GET /api/v1/ledger/internal-accounts/trial-balance/{}", currency);

        List<LedgerInternalAccount> internalAccounts = ledgerInternalAccountService.getInternalAccounts(currency);
        BigDecimal internalAccountsNet = ledgerInternalAccountService.netBalance(currency);
        BigDecimal customerWalletsNet = ledgerAccountService.netBalanceOfCustomerWallets(currency);
        BigDecimal net = internalAccountsNet.add(customerWalletsNet);

        return ResponseEntity.ok(TrialBalanceResponse.builder()
                .currency(currency)
                .internalAccountsNet(internalAccountsNet)
                .customerWalletsNet(customerWalletsNet)
                .net(net)
                .balanced(net.signum() == 0)
                .internalAccounts(internalAccounts.stream().map(LedgerAccountMapper::toLedgerInternalAccountResponse).toList())
                .build());
    }
}
