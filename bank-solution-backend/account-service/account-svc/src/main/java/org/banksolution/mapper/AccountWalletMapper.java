package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.Currency;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.banksolution.model.response.AccountWalletResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UtilityClass
public class AccountWalletMapper {

    public static List<AccountWalletEntity> toAccountWalletEntities(
            AccountEntity accountEntity,
            List<Currency> currencies,
            List<LedgerAccountResponse> ledgerAccountResponses) {

        Map<Currency, UUID> ledgerAccountIdsByCurrency = toLedgerAccountIdsByCurrency(ledgerAccountResponses);
        Currency primaryCurrency = currencies.getFirst();

        return currencies.stream()
                .map(currency -> toAccountWalletEntity(
                        accountEntity,
                        currency,
                        ledgerAccountIdsByCurrency.get(currency),
                        currency == primaryCurrency))
                .toList();
    }

    public static AccountWalletResponse toAccountWalletResponse(AccountWalletEntity accountWalletEntity) {
        return AccountWalletResponse.builder()
                .id(accountWalletEntity.getId())
                .ledgerAccountId(accountWalletEntity.getLedgerAccountId())
                .currency(accountWalletEntity.getCurrency())
                .walletStatus(accountWalletEntity.getWalletStatus())
                .balance(accountWalletEntity.getBalance())
                .availableBalance(accountWalletEntity.getAvailableBalance())
                .primary(accountWalletEntity.isPrimary())
                .build();
    }

    private static Map<Currency, UUID> toLedgerAccountIdsByCurrency(
            List<LedgerAccountResponse> ledgerAccountResponses) {

        return ledgerAccountResponses.stream()
                .collect(Collectors.toMap(
                        LedgerAccountResponse::getCurrency,
                        LedgerAccountResponse::getLedgerAccountId,
                        (existingLedgerAccountId, _) -> existingLedgerAccountId));
    }

    private static AccountWalletEntity toAccountWalletEntity(
            AccountEntity accountEntity,
            Currency currency,
            UUID ledgerAccountId,
            boolean primary) {

        return AccountWalletEntity.builder()
                .account(accountEntity)
                .ledgerAccountId(ledgerAccountId)
                .currency(currency)
                .primary(primary)
                .build();
    }
}
