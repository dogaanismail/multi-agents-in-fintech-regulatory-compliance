package org.banksolution.integration.account.dto;

import lombok.*;
import org.banksolution.enums.AccountStatus;
import org.banksolution.enums.AccountType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private UUID id;
    private UUID customerId;
    private String accountNumber;
    private AccountType accountType;
    private String bankLocation;
    private AccountStatus accountStatus;
    private LocalDate openingDate;
    private LocalDate closingDate;
    private List<BalanceResponse> balances;
    private Instant createdAt;
    private Instant updatedAt;

}
