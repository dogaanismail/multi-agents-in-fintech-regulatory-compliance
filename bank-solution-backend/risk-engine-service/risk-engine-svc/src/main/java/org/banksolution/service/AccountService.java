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

    public AccountResponse getAccountById(UUID accountId) {
        return accountServiceClient.getAccountById(accountId);
    }

    public List<AccountResponse> getAccountsByIds(List<UUID> accountIds) {
        return accountServiceClient.getAccountsByIds(accountIds);
    }
}
