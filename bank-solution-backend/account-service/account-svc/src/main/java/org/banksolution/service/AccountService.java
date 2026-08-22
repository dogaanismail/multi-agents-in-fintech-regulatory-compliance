package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.AccountNumberGenerationException;
import org.banksolution.exception.CustomerNotFoundException;
import org.banksolution.mapper.AccountMapper;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.repository.AccountRepository;
import org.banksolution.utils.AccountNumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 5;

    private final AccountRepository accountRepository;
    private final AccountWalletService accountWalletService;
    private final CustomerClientService customerClientService;

    @Transactional
    public AccountResponse openAccount(OpenAccountRequest request) {
        log.info("Opening account for customer: {}", request.getCustomerId());

        if (!customerClientService.customerExists(request.getCustomerId())) {
            throw new CustomerNotFoundException(request.getCustomerId());
        }

        String accountNumber = generateUniqueAccountNumber();

        AccountEntity account = accountRepository.save(AccountMapper.toAccountEntity(request, accountNumber));
        List<AccountWalletEntity> wallets = accountWalletService.openWallets(account, request.getCurrencies());

        log.info("Account opened successfully with account number: {}", accountNumber);

        return AccountMapper.toAccountResponse(account, wallets);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        log.info("Fetching account with id: {}", id);

        AccountEntity account = accountRepository.findActiveById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        return AccountMapper.toAccountResponse(account, account.getWallets());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getByAccountIds(List<UUID> ids) {
        log.info("Fetching accounts with ids: {}", ids);

        return accountRepository.findActiveByIdIn(ids)
                .stream()
                .map(account -> AccountMapper.toAccountResponse(account, account.getWallets()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(UUID customerId) {
        log.info("Fetching accounts for customer: {}", customerId);

        return accountRepository.findActiveByCustomerId(customerId)
                .stream()
                .map(account -> AccountMapper.toAccountResponse(account, account.getWallets()))
                .toList();
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 1; attempt <= MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            String accountNumber = AccountNumberUtils.generateAccountNumber();

            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }

            log.warn("Generated account number {} is already taken, attempt {} of {}",
                    accountNumber, attempt, MAX_ACCOUNT_NUMBER_ATTEMPTS);
        }

        throw new AccountNumberGenerationException(MAX_ACCOUNT_NUMBER_ATTEMPTS);
    }
}
