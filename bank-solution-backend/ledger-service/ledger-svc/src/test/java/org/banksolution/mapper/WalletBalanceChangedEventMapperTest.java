package org.banksolution.mapper;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.LedgerAccountFixtures.createWalletWithBalances;
import static org.banksolution.mapper.WalletBalanceChangedEventMapper.toWalletBalanceChangedEvent;

class WalletBalanceChangedEventMapperTest {

    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.randomUUID();

    @Test
    void shouldIdentifyTheWalletByItsDerivedLedgerAccountId() {
        WalletBalanceChangedEvent event = toWalletBalanceChangedEvent(createWalletWithBalances(
                CUSTOMER_ACCOUNT_ID, Currency.GBP, "1000.00", "250.00", "100.00", "0.00"));

        assertThat(event.getLedgerAccountId()).isEqualTo(
                LedgerAccountIds.deriveWalletAccountId(CUSTOMER_ACCOUNT_ID, Currency.GBP).toString());
        assertThat(event.getCustomerAccountId()).isEqualTo(CUSTOMER_ACCOUNT_ID.toString());
        assertThat(event.getCurrency()).isEqualTo("GBP");
    }

    @Test
    void shouldReportThePostedBalanceAsCreditsMinusDebits() {
        WalletBalanceChangedEvent event = toWalletBalanceChangedEvent(createWalletWithBalances(
                CUSTOMER_ACCOUNT_ID, Currency.GBP, "1000.00", "250.00", "0.00", "0.00"));

        assertThat(event.getPostedBalance()).isEqualTo("750.00");
    }

    @Test
    void shouldSubtractPendingDebitsFromTheAvailableBalance() {
        WalletBalanceChangedEvent event = toWalletBalanceChangedEvent(createWalletWithBalances(
                CUSTOMER_ACCOUNT_ID, Currency.GBP, "1000.00", "250.00", "100.00", "0.00"));

        assertThat(event.getPostedBalance()).isEqualTo("750.00");
        assertThat(event.getAvailableBalance()).isEqualTo("650.00");
        assertThat(event.getPendingDebits()).isEqualTo("100.00");
    }

    @Test
    void shouldNotCountPendingCreditsAsSpendable() {
        WalletBalanceChangedEvent event = toWalletBalanceChangedEvent(createWalletWithBalances(
                CUSTOMER_ACCOUNT_ID, Currency.GBP, "1000.00", "0.00", "0.00", "300.00"));

        assertThat(event.getAvailableBalance()).isEqualTo("1000.00");
        assertThat(event.getPendingCredits()).isEqualTo("300.00");
    }

    @Test
    void shouldPreserveMinorUnitPrecisionAcrossCurrencyExponents() {
        WalletBalanceChangedEvent gbpEvent = toWalletBalanceChangedEvent(createWalletWithBalances(
                CUSTOMER_ACCOUNT_ID, Currency.GBP, "0.01", "0.00", "0.00", "0.00"));
        WalletBalanceChangedEvent jpyEvent = toWalletBalanceChangedEvent(createWalletWithBalances(
                CUSTOMER_ACCOUNT_ID, Currency.JPY, "500", "0", "0", "0"));

        assertThat(gbpEvent.getPostedBalance()).isEqualTo("0.01");
        assertThat(jpyEvent.getPostedBalance()).isEqualTo("500");
    }
}
