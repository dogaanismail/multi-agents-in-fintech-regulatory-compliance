package org.banksolution.service;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.exception.LedgerAccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerInternalAccountServiceTest extends BaseIntegrationTest {

    private static final int SEEDED_CURRENCIES = 3;

    @Autowired
    private LedgerInternalAccountService ledgerInternalAccountService;

    @Test
    void shouldCreateAnInternalAccountWithADerivedId() {
        LedgerInternalAccount internalAccount =
                ledgerInternalAccountService.createInternalAccount(LedgerAccountType.SUSPENSE, Currency.MXN);

        assertThat(internalAccount.id())
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.SUSPENSE, Currency.MXN));
        assertThat(internalAccount.netBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRefuseToCreateACustomerAccountTypeAsInternal() {
        assertThatThrownBy(() ->
                ledgerInternalAccountService.createInternalAccount(LedgerAccountType.WALLET, Currency.GBP))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an internal account type");
    }

    @Test
    void shouldRetrieveAnInternalAccountByTypeAndCurrency() {
        LedgerInternalAccount internalAccount = ledgerInternalAccountService
                .getInternalAccount(LedgerAccountType.INBOUND_CLEARING, Currency.GBP);

        assertThat(internalAccount.accountType()).isEqualTo(LedgerAccountType.INBOUND_CLEARING);
        assertThat(internalAccount.currency()).isEqualTo(Currency.GBP);
    }

    @Test
    void shouldRetrieveAnInternalAccountByItsLedgerAccountId() {
        UUID ledgerAccountId =
                LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.FEES_INCOME, Currency.EUR);

        assertThat(ledgerInternalAccountService.getInternalAccount(ledgerAccountId).accountType())
                .isEqualTo(LedgerAccountType.FEES_INCOME);
    }

    @Test
    void shouldListTheSeededChartOfAccountsForOneCurrency() {
        assertThat(ledgerInternalAccountService.getInternalAccounts(Currency.GBP))
                .hasSize(LedgerAccountType.internalTypes().length)
                .allMatch(internalAccount -> internalAccount.currency() == Currency.GBP);
    }

    @Test
    void shouldListTheSeededChartOfAccountsAcrossEveryCurrency() {
        assertThat(ledgerInternalAccountService.getInternalAccounts())
                .hasSizeGreaterThanOrEqualTo(SEEDED_CURRENCIES * LedgerAccountType.internalTypes().length);
    }

    @Test
    void shouldFailWhenTheInternalAccountDoesNotExist() {
        UUID unknownLedgerAccountId = UUID.randomUUID();

        assertThatThrownBy(() -> ledgerInternalAccountService.getInternalAccount(unknownLedgerAccountId))
                .isInstanceOf(LedgerAccountNotFoundException.class);
    }

    @Test
    void shouldReportABalancedChartOfAccountsBeforeAnyCustomerPostings() {
        assertThat(ledgerInternalAccountService.netBalance(Currency.NGN)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
