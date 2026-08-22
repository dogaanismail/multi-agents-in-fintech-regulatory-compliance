package org.banksolution.service;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.exception.LedgerAccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerAccountServiceTest extends BaseIntegrationTest {

    @Autowired
    private LedgerAccountService ledgerAccountService;

    @Test
    void shouldCreateAWalletWithADerivedId() {
        UUID customerAccountId = UUID.randomUUID();

        LedgerAccount wallet = ledgerAccountService.createLedgerAccount(customerAccountId, Currency.GBP);

        assertThat(wallet.id()).isEqualTo(LedgerAccountIds.deriveWalletAccountId(customerAccountId, Currency.GBP));
        assertThat(wallet.accountType()).isEqualTo(LedgerAccountType.WALLET);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldCreateWalletsForSeveralCurrenciesInOneCall() {
        UUID customerAccountId = UUID.randomUUID();

        List<LedgerAccount> wallets = ledgerAccountService.createLedgerAccounts(List.of(
                LedgerAccount.newWallet(customerAccountId, Currency.GBP),
                LedgerAccount.newWallet(customerAccountId, Currency.EUR)));

        assertThat(wallets)
                .extracting(LedgerAccount::currency)
                .containsExactlyInAnyOrder(Currency.GBP, Currency.EUR);
    }

    @Test
    void shouldRetrieveAWalletByItsLedgerAccountId() {
        UUID customerAccountId = UUID.randomUUID();
        LedgerAccount created = ledgerAccountService.createLedgerAccount(customerAccountId, Currency.USD);

        assertThat(ledgerAccountService.getLedgerAccount(created.id()).accountId()).isEqualTo(customerAccountId);
    }

    @Test
    void shouldRetrieveAWalletByCustomerAccountAndCurrency() {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, Currency.USD);

        assertThat(ledgerAccountService.getWallet(customerAccountId, Currency.USD).currency())
                .isEqualTo(Currency.USD);
    }

    @Test
    void shouldRetrieveOnlyTheCurrenciesTheCustomerActuallyHolds() {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, Currency.GBP);
        ledgerAccountService.createLedgerAccount(customerAccountId, Currency.JPY);

        assertThat(ledgerAccountService.getWallets(customerAccountId))
                .extracting(LedgerAccount::currency)
                .containsExactlyInAnyOrder(Currency.GBP, Currency.JPY);
    }

    @Test
    void shouldReturnNoWalletsForACustomerWithoutAny() {
        assertThat(ledgerAccountService.getWallets(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldFailWhenTheLedgerAccountDoesNotExist() {
        UUID unknownLedgerAccountId = UUID.randomUUID();

        assertThatThrownBy(() -> ledgerAccountService.getLedgerAccount(unknownLedgerAccountId))
                .isInstanceOf(LedgerAccountNotFoundException.class);
    }

    @Test
    void shouldFailWhenTheCustomerHasNoWalletInThatCurrency() {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, Currency.GBP);

        assertThatThrownBy(() -> ledgerAccountService.getWallet(customerAccountId, Currency.TRY))
                .isInstanceOf(LedgerAccountNotFoundException.class);
    }
}
