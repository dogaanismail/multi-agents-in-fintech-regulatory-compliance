package org.banksolution.fixtures;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.banksolution.entity.AccountEntity;
import org.banksolution.entity.AccountWalletEntity;
import org.banksolution.enums.AccountType;
import org.banksolution.enums.BankLocation;
import org.banksolution.enums.Currency;
import org.banksolution.integration.customer.dto.AddressResponse;
import org.banksolution.integration.customer.dto.CustomerResponse;
import org.banksolution.integration.customer.dto.CustomerStatus;
import org.banksolution.integration.customer.dto.CustomerType;
import org.banksolution.integration.ledger.dto.LedgerAccountResponse;
import org.banksolution.model.request.OpenAccountRequest;
import org.banksolution.utils.AccountNumberUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AccountFixtures {

    public static final LocalDate OPENING_DATE = LocalDate.of(2026, 8, 26);

    private AccountFixtures() {
    }

    public static OpenAccountRequest createOpenAccountRequest(UUID customerId, List<Currency> currencies) {
        return OpenAccountRequest.builder()
                .customerId(customerId)
                .accountType(AccountType.CHECKING)
                .bankLocation(BankLocation.GB)
                .currencies(currencies)
                .build();
    }

    public static AccountEntity createAccountEntity(UUID customerId) {
        return AccountEntity.builder()
                .customerId(customerId)
                .accountNumber(AccountNumberUtils.generateAccountNumber())
                .bankLocation(BankLocation.GB)
                .accountType(AccountType.CHECKING)
                .openingDate(OPENING_DATE)
                .build();
    }

    public static AccountEntity createPersistedAccountEntity(UUID accountId, UUID customerId) {
        AccountEntity accountEntity = createAccountEntity(customerId);
        accountEntity.setId(accountId);
        return accountEntity;
    }

    public static AccountWalletEntity createAccountWalletEntity(
            AccountEntity accountEntity,
            Currency currency,
            boolean primary) {

        return AccountWalletEntity.builder()
                .account(accountEntity)
                .ledgerAccountId(UUID.randomUUID())
                .currency(currency)
                .primary(primary)
                .build();
    }

    public static AccountWalletEntity createPersistedAccountWalletEntity(
            AccountEntity accountEntity,
            Currency currency,
            boolean primary) {

        AccountWalletEntity accountWalletEntity = createAccountWalletEntity(accountEntity, currency, primary);
        accountWalletEntity.setId(UUID.randomUUID());
        return accountWalletEntity;
    }

    public static LedgerAccountResponse createLedgerAccountResponse(UUID accountId, Currency currency) {
        return LedgerAccountResponse.builder()
                .ledgerAccountId(UUID.randomUUID())
                .accountId(accountId)
                .currency(currency)
                .build();
    }

    public static List<LedgerAccountResponse> createLedgerAccountResponses(UUID accountId, List<Currency> currencies) {
        return currencies.stream()
                .map(currency -> createLedgerAccountResponse(accountId, currency))
                .toList();
    }

    public static CustomerResponse createCustomerResponse(UUID customerId) {
        return CustomerResponse.builder()
                .id(customerId)
                .firstName("Alice")
                .lastName("Johnson")
                .email("alice." + customerId + "@example.com")
                .phoneNumber("+37255512345")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .nationality("EE")
                .customerType(CustomerType.INDIVIDUAL)
                .customerStatus(CustomerStatus.ACTIVE)
                .address(AddressResponse.builder()
                        .id(UUID.randomUUID())
                        .city("Tallinn")
                        .countryCode("EE")
                        .build())
                .build();
    }

    public static WalletBalanceChangedEvent createWalletBalanceChangedEvent(
            String ledgerAccountId,
            String postedBalance,
            String availableBalance) {

        return WalletBalanceChangedEvent.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setLedgerAccountId(ledgerAccountId)
                .setCustomerAccountId(UUID.randomUUID().toString())
                .setCurrency(Currency.GBP.name())
                .setPostedBalance(postedBalance)
                .setAvailableBalance(availableBalance)
                .setPendingDebits("100.00")
                .setPendingCredits("0.00")
                .setTimestamp(OPENING_DATE.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
    }
}
