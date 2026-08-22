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
            AccountEntity account,
            List<Currency> currencies,
            List<LedgerAccountResponse> ledgerAccounts) {

        Map<Currency, UUID> ledgerAccountIdsByCurrency = toLedgerAccountIdsByCurrency(ledgerAccounts);
        Currency primaryCurrency = currencies.getFirst();

        return currencies.stream()
                .map(currency -> toAccountWalletEntity(
                        account,
                        currency,
                        ledgerAccountIdsByCurrency.get(currency),
                        currency == primaryCurrency))
                .toList();
    }

    public static AccountWalletResponse toAccountWalletResponse(AccountWalletEntity entity) {
        return AccountWalletResponse.builder()
                .id(entity.getId())
                .ledgerAccountId(entity.getLedgerAccountId())
                .currency(entity.getCurrency())
                .walletStatus(entity.getWalletStatus())
                .balance(entity.getBalance())
                .primary(entity.isPrimary())
                .build();
    }

    private static Map<Currency, UUID> toLedgerAccountIdsByCurrency(
            List<LedgerAccountResponse> ledgerAccounts) {

        return ledgerAccounts.stream()
                .collect(Collectors.toMap(
                        LedgerAccountResponse::getCurrency,
                        LedgerAccountResponse::getLedgerAccountId,
                        (existing, _) -> existing));
    }

    private static AccountWalletEntity toAccountWalletEntity(
            AccountEntity account,
            Currency currency,
            UUID ledgerAccountId,
            boolean primary) {

        return AccountWalletEntity.builder()
                .account(account)
                .ledgerAccountId(ledgerAccountId)
                .currency(currency)
                .primary(primary)
                .build();
    }
}
