package org.banksolution.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.integration.account.AccountServiceClient;
import org.banksolution.integration.account.model.AccountResponse;
import org.banksolution.mapper.AccountMapper;
import org.banksolution.model.request.MobileOpenAccountRequest;
import org.banksolution.model.response.MobileAccountResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CustomerIdentityService customerIdentityService;
    private final AccountServiceClient accountServiceClient;

    public List<MobileAccountResponse> getAccounts(Jwt caller) {
        UUID customerId = customerIdentityService.getOnboardedCustomer(caller).id();
        return accountServiceClient.getAccountsByCustomerId(customerId).stream()
                .map(AccountMapper::toMobileAccountResponse)
                .toList();
    }

    public MobileAccountResponse getAccount(
            Jwt caller,
            UUID accountId) {

        UUID customerId = customerIdentityService.getOnboardedCustomer(caller).id();
        return AccountMapper.toMobileAccountResponse(requireAccountOwnedBy(customerId, accountId));
    }

    public MobileAccountResponse openAccount(
            Jwt caller,
            MobileOpenAccountRequest mobileOpenAccountRequest) {

        UUID customerId = customerIdentityService.getOnboardedCustomer(caller).id();
        AccountResponse openedAccount = accountServiceClient.openAccount(
                AccountMapper.toOpenAccountRequest(customerId, mobileOpenAccountRequest));
        return AccountMapper.toMobileAccountResponse(openedAccount);
    }

    public AccountResponse requireAccountOwnedBy(
            UUID customerId,
            UUID accountId) {

        AccountResponse accountResponse;
        try {
            accountResponse = accountServiceClient.getAccountById(accountId);
        } catch (FeignException.NotFound _) {
            throw new AccountNotFoundException(accountId);
        }

        if (!customerId.equals(accountResponse.customerId())) {
            throw new AccountNotFoundException(accountId);
        }

        return accountResponse;
    }
}
