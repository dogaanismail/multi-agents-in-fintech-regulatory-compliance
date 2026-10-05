package org.banksolution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.banksolution.model.request.MobileOpenAccountRequest;
import org.banksolution.model.response.MobileAccountResponse;
import org.banksolution.service.AccountService;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/accounts")
@Tag(name = "Accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "List my accounts")
    public ResponseEntity<@NonNull List<MobileAccountResponse>> getAccounts(@AuthenticationPrincipal Jwt caller) {
        return ResponseEntity.ok(accountService.getAccounts(caller));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get one of my accounts with its balances")
    public ResponseEntity<@NonNull MobileAccountResponse> getAccount(
            @AuthenticationPrincipal Jwt caller,
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(accountService.getAccount(caller, accountId));
    }

    @PostMapping
    @Operation(summary = "Open an account")
    public ResponseEntity<@NonNull MobileAccountResponse> openAccount(
            @AuthenticationPrincipal Jwt caller,
            @Valid @RequestBody MobileOpenAccountRequest mobileOpenAccountRequest) {

        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.openAccount(caller, mobileOpenAccountRequest));
    }
}
