package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AccountBalanceEntity;
import org.banksolution.entity.AccountEntity;
import org.banksolution.enums.Currency;
import org.banksolution.exception.AccountAlreadyExistsException;
import org.banksolution.exception.AccountNotFoundException;
import org.banksolution.exception.BalanceNotFoundException;
import org.banksolution.integration.customer.CustomerServiceClient;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.banksolution.mapper.AccountMapper;
import org.banksolution.mapper.BalanceMapper;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.model.response.AccountResponse;
import org.banksolution.model.response.BalanceResponse;
import org.banksolution.repository.AccountBalanceRepository;
import org.banksolution.repository.AccountRepository;
import org.banksolution.utils.AccountNumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.banksolution.mapper.AccountMapper.toEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final CustomerServiceClient customerServiceClient;

    @Transactional
    public AccountResponse openAccount(OpenAccountRequest request) {
        log.info("Opening account for customer: {}", request.getCustomerId());

        String accountNumber = AccountNumberUtils.generateAccountNumber();

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new AccountAlreadyExistsException(accountNumber);
        }

        if (!customerExists(request.getCustomerId())) {
            throw new IllegalArgumentException("Customer with ID " + request.getCustomerId() + " does not exist.");
        }

        AccountEntity entity = toEntity(request, accountNumber);
        AccountEntity savedEntity = accountRepository.save(entity);

        log.info("Account opened successfully with account number: {}", accountNumber);
        return AccountMapper.toResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        log.info("Fetching account with id: {}", id);

        AccountEntity entity = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        return AccountMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(UUID customerId) {
        log.info("Fetching accounts for customer: {}", customerId);

        List<AccountEntity> accounts = accountRepository.findByCustomerId(customerId);

        return accounts.stream()
                .map(AccountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getBalancesByAccountId(UUID accountId) {
        log.info("Fetching balances for account: {}", accountId);

        List<AccountBalanceEntity> balances = accountBalanceRepository.findByAccountId(accountId);

        return balances.stream()
                .map(BalanceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalanceByCurrency(UUID accountId, Currency currency) {
        log.info("Fetching balance for account: {} and currency: {}", accountId, currency);

        AccountBalanceEntity balance = accountBalanceRepository.findByAccountIdAndCurrency(accountId, currency)
                .orElseThrow(() -> new BalanceNotFoundException(accountId, currency));

        return BalanceMapper.toResponse(balance);
    }

    private boolean customerExists(UUID customerId) {
        try {
            CustomerResponse customerResponse = customerServiceClient.getCustomerById(customerId);
            return customerResponse != null;
        } catch (Exception e) {
            log.error("Error while verifying customer existence: {}", e.getMessage());
            return false;
        }
    }

}

