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

import java.time.Clock;
import java.time.LocalDate;
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
    private final Clock clock;

    @Transactional
    public AccountResponse openAccount(OpenAccountRequest openAccountRequest) {
        log.info("Opening account for customer: {}", openAccountRequest.getCustomerId());

        if (!customerClientService.customerExists(openAccountRequest.getCustomerId())) {
            throw new CustomerNotFoundException(openAccountRequest.getCustomerId());
        }

        String accountNumber = generateUniqueAccountNumber();

        AccountEntity accountEntity = accountRepository.save(
                AccountMapper.toAccountEntity(openAccountRequest, accountNumber, LocalDate.now(clock)));
        List<AccountWalletEntity> accountWalletEntities =
                accountWalletService.openWallets(accountEntity, openAccountRequest.getCurrencies());

        log.info("Account opened successfully with account number: {}", accountNumber);

        return AccountMapper.toAccountResponse(accountEntity, accountWalletEntities);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        log.info("Fetching account with id: {}", accountId);

        AccountEntity accountEntity = accountRepository.findActiveById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return AccountMapper.toAccountResponse(accountEntity, accountEntity.getWallets());
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getByAccountIds(List<UUID> accountIds) {
        log.info("Fetching accounts with ids: {}", accountIds);

        return accountRepository.findActiveByIdIn(accountIds)
                .stream()
                .map(accountEntity -> AccountMapper.toAccountResponse(accountEntity, accountEntity.getWallets()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(UUID customerId) {
        log.info("Fetching accounts for customer: {}", customerId);

        return accountRepository.findActiveByCustomerId(customerId)
                .stream()
                .map(accountEntity -> AccountMapper.toAccountResponse(accountEntity, accountEntity.getWallets()))
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
