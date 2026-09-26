package org.banksolution.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Bank account with its wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    @Schema(description = "Account id", example = "7b1e4d2a-6c3f-4a8e-b5d9-2e0f8c1a4b67")
    private UUID id;

    @Schema(description = "Owning customer's id", example = "3f2a7c1e-8b4d-4e6a-9c2f-1d5e7b9a0c34")
    private UUID customerId;

    @Schema(description = "Ten-digit account number", example = "4829175036")
    private String accountNumber;

    @Schema(description = "Kind of account", example = "CHECKING")
    private AccountType accountType;

    @Schema(description = "Country of the holding branch", example = "GB")
    private String bankLocation;

    @Schema(description = "Account's lifecycle status", example = "ACTIVE")
    private AccountStatus accountStatus;

    @Schema(description = "Date the account was opened", example = "2026-01-15")
    private LocalDate openingDate;

    @Schema(description = "Date the account was closed", example = "2026-12-31")
    private LocalDate closingDate;

    @Schema(description = "Per-currency wallets of the account")
    private List<AccountWalletResponse> wallets;

    @Schema(description = "When the account was created", example = "2026-01-15T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "When the account was last updated", example = "2026-01-15T10:30:00Z")
    private Instant updatedAt;

}

