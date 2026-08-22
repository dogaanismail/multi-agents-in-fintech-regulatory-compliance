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
            AccountEntity account,
            List<Currency> currencies) {

        List<Currency> walletCurrencies = currencies.stream().distinct().toList();

        List<LedgerAccountResponse> ledgerAccounts =
                ledgerClientService.openLedgerWallets(account.getId(), walletCurrencies);

        List<AccountWalletEntity> wallets =
                AccountWalletMapper.toAccountWalletEntities(account, walletCurrencies, ledgerAccounts);

        List<AccountWalletEntity> openedWallets = accountWalletRepository.saveAll(wallets);

        log.info("Opened {} wallet(s) for account: {}", openedWallets.size(), account.getId());

        return openedWallets;
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

        log.info("Fetching wallet for account: {} and currency: {}",
                accountId,
                currency);

        AccountWalletEntity wallet = accountWalletRepository.findByAccountIdAndCurrency(accountId, currency)
                .orElseThrow(() -> new WalletNotFoundException(accountId, currency));

        return AccountWalletMapper.toAccountWalletResponse(wallet);
    }
}
