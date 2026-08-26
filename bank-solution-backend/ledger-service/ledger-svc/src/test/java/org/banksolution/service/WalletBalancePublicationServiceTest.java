package org.banksolution.service;

import com.aml.ledger.WalletBalanceChangedEvent;
import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.infrastructure.messaging.kafka.producer.WalletBalanceChangedEventProducer;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class WalletBalancePublicationServiceTest extends BaseIntegrationTest {

    @MockitoBean
    private WalletBalanceChangedEventProducer walletBalanceChangedEventProducer;

    private static final Currency CURRENCY = Currency.GBP;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AUTHORISED_AMOUNT = new BigDecimal("250.00");

    @Autowired
    private LedgerPostingService ledgerPostingService;

    @Autowired
    private LedgerAccountService ledgerAccountService;

    @Test
    void shouldPublishTheReducedAvailableBalanceWhenFundsAreHeld() {
        UUID customerAccountId = givenFundedWallet();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.outboundAuthorisation(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));

        WalletBalanceChangedEvent walletBalanceChangedEvent = lastEventForWallet(customerAccountId, CURRENCY);
        assertThat(walletBalanceChangedEvent.getPostedBalance()).isEqualTo("1000.00");
        assertThat(walletBalanceChangedEvent.getAvailableBalance()).isEqualTo("750.00");
        assertThat(walletBalanceChangedEvent.getPendingDebits()).isEqualTo("250.00");
    }

    @Test
    void shouldPublishTheSettledBalanceByResolvingTheAuthorisationLeg() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.outboundAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        WalletBalanceChangedEvent walletBalanceChangedEvent = lastEventForWallet(customerAccountId, CURRENCY);
        assertThat(walletBalanceChangedEvent.getPostedBalance()).isEqualTo("750.00");
        assertThat(walletBalanceChangedEvent.getAvailableBalance()).isEqualTo("750.00");
        assertThat(walletBalanceChangedEvent.getPendingDebits()).isEqualTo("0.00");
    }

    @Test
    void shouldPublishTheRestoredBalanceWhenTheAuthorisationIsReleased() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.outboundAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.release(clientTransactionId));

        WalletBalanceChangedEvent walletBalanceChangedEvent = lastEventForWallet(customerAccountId, CURRENCY);
        assertThat(walletBalanceChangedEvent.getPostedBalance()).isEqualTo("1000.00");
        assertThat(walletBalanceChangedEvent.getAvailableBalance()).isEqualTo("1000.00");
    }

    @Test
    void shouldPublishBothWalletsOfAnInternalTransfer() {
        UUID payerAccountId = givenFundedWallet();
        UUID payeeAccountId = givenWallet(CURRENCY);

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.internalTransferAuthorisation(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, payerAccountId, payeeAccountId));

        assertThat(lastEventForWallet(payerAccountId, CURRENCY).getAvailableBalance()).isEqualTo("750.00");
        assertThat(lastEventForWallet(payeeAccountId, CURRENCY).getPendingCredits()).isEqualTo("250.00");
    }

    @Test
    void shouldPublishBothCurrencyWalletsOfACrossCurrencyTransfer() {
        UUID payerAccountId = givenFundedWallet();
        UUID payeeAccountId = givenWallet(Currency.EUR);
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.crossCurrencyTransferAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY,
                new BigDecimal("290.00"), Currency.EUR, payerAccountId, payeeAccountId));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        assertThat(lastEventForWallet(payerAccountId, CURRENCY).getPostedBalance()).isEqualTo("750.00");
        assertThat(lastEventForWallet(payeeAccountId, Currency.EUR).getPostedBalance()).isEqualTo("290.00");
    }

    @Test
    void shouldNeverPublishBalancesOfInternalAccounts() {
        UUID customerAccountId = givenFundedWallet();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.outboundAuthorisation(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));

        UUID walletLedgerAccountId = LedgerAccountIds.deriveWalletAccountId(customerAccountId, CURRENCY);
        assertThat(capturedEvents())
                .extracting(WalletBalanceChangedEvent::getLedgerAccountId)
                .containsOnly(walletLedgerAccountId.toString());
    }

    private UUID givenFundedWallet() {
        UUID customerAccountId = givenWallet(CURRENCY);
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.inboundHardSettlement(
                UUID.randomUUID(), OPENING_BALANCE, CURRENCY, customerAccountId, null));
        return customerAccountId;
    }

    private UUID givenWallet(Currency currency) {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, currency);
        return customerAccountId;
    }

    private List<WalletBalanceChangedEvent> capturedEvents() {
        ArgumentCaptor<WalletBalanceChangedEvent> walletBalanceChangedEventCaptor =
                ArgumentCaptor.forClass(WalletBalanceChangedEvent.class);
        verify(walletBalanceChangedEventProducer, atLeastOnce()).publish(walletBalanceChangedEventCaptor.capture());
        return walletBalanceChangedEventCaptor.getAllValues();
    }

    private WalletBalanceChangedEvent lastEventForWallet(UUID customerAccountId, Currency currency) {
        String walletLedgerAccountId =
                LedgerAccountIds.deriveWalletAccountId(customerAccountId, currency).toString();

        return capturedEvents().stream()
                .filter(walletBalanceChangedEvent -> walletBalanceChangedEvent.getLedgerAccountId().equals(walletLedgerAccountId))
                .reduce((_, second) -> second)
                .orElseThrow();
    }
}
