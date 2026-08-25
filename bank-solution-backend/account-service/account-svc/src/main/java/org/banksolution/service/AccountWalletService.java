package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.exception.WalletNotFoundException;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.banksolution.mapper.AccountWalletMapper;
import org.banksolution.model.response.AccountWalletResponse;
import org.banksolution.repository.AccountWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountWalletService {

    private final AccountWalletRepository accountWalletRepository;
    private final LedgerClientService ledgerClientService;

    @Transactional
    public List<AccountWalletEntity> openWallets(
            AccountEntity accountEntity,
            List<Currency> currencies) {

        List<Currency> walletCurrencies = currencies.stream().distinct().toList();

        List<LedgerAccountResponse> ledgerAccountResponses =
                ledgerClientService.openLedgerWallets(accountEntity.getId(), walletCurrencies);

        List<AccountWalletEntity> accountWalletEntities =
                AccountWalletMapper.toAccountWalletEntities(accountEntity, walletCurrencies, ledgerAccountResponses);

        List<AccountWalletEntity> openedAccountWalletEntities = accountWalletRepository.saveAll(accountWalletEntities);

        log.info("Opened {} wallet(s) for account: {}", openedAccountWalletEntities.size(), accountEntity.getId());

        return openedAccountWalletEntities;
    }

    @Transactional(readOnly = true)
    public List<AccountWalletResponse> getWalletsByAccountId(UUID accountId) {
        log.info("Fetching wallets for account: {}", accountId);

        return accountWalletRepository.findByAccountId(accountId)
                .stream()
                .map(AccountWalletMapper::toAccountWalletResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountWalletResponse getWalletByCurrency(
            UUID accountId,
            Currency currency) {

        log.info("Fetching wallet for account: {} and currency: {}", accountId, currency);

        AccountWalletEntity accountWalletEntity = accountWalletRepository.findByAccountIdAndCurrency(accountId, currency)
                .orElseThrow(() -> new WalletNotFoundException(accountId, currency));

        return AccountWalletMapper.toAccountWalletResponse(accountWalletEntity);
    }
}
