package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import org.banksolution.integration.account.AccountServiceClient;
import org.banksolution.integration.account.dto.AccountResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountServiceClient accountServiceClient;

    public boolean isCrossOrderPayment(UUID sourceAccountId, UUID destinationAccountId) {
        List<UUID> accountIds = List.of(sourceAccountId, destinationAccountId);
        List<AccountResponse> accounts = accountServiceClient.getAccountsByIds(accountIds);

        String sourceLocation = accounts.stream()
                .filter(acc -> acc.getId().equals(sourceAccountId))
                .map(AccountResponse::getBankLocation)
                .findFirst()
                .orElse(null);

        String destinationLocation = accounts.stream()
                .filter(acc -> acc.getId().equals(destinationAccountId))
                .map(AccountResponse::getBankLocation)
                .findFirst()
                .orElse(null);

        if (sourceLocation == null || destinationLocation == null) {
            return false;
        }

        return !sourceLocation.equalsIgnoreCase(destinationLocation);
    }
}

