package org.banksolution.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.enums.Currency;
import org.banksolution.exception.WalletCreationFailedException;
import org.banksolution.integration.ledger.LedgerServiceClient;
import org.banksolution.integration.ledger.dto.CreateLedgerAccountsRequest;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerClientService {

    private final LedgerServiceClient ledgerServiceClient;

    public List<LedgerAccountResponse> openLedgerWallets(
            UUID accountId,
            List<Currency> currencies) {

        try {
            List<LedgerAccountResponse> ledgerAccounts = ledgerServiceClient.createLedgerAccounts(
                    CreateLedgerAccountsRequest.forCurrencies(accountId, currencies));

            log.info("Opened {} ledger wallet(s) for account: {}, currencies: {}",
                    ledgerAccounts.size(),
                    accountId,
                    currencies);

            return ledgerAccounts;
        } catch (FeignException e) {
            log.error("Failed to open ledger wallets for account: {}, status: {}", accountId, e.status(), e);
            throw new WalletCreationFailedException(accountId, e);
        }
    }
}
