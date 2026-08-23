package org.banksolution.service;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.exception.InsufficientLedgerFundsException;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.enums.PostingInstructionType.*;

class LedgerPostingServiceTest extends BaseIntegrationTest {

    private static final Currency CURRENCY = Currency.GBP;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AUTHORISED_AMOUNT = new BigDecimal("250.00");
    private static final BigDecimal MORE_THAN_THE_BALANCE = new BigDecimal("100000.00");

    @Autowired
    private LedgerPostingService ledgerPostingService;

    @Autowired
    private LedgerAccountService ledgerAccountService;

    @Autowired
    private LedgerInternalAccountService ledgerInternalAccountService;

    @Test
    void shouldHoldFundsOnOutboundAuthorisationWithoutMovingThem() {
        UUID customerAccountId = givenFundedWallet();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(UUID.randomUUID(), customerAccountId, AUTHORISED_AMOUNT));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.debitsPending()).isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(wallet.debitsPosted()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE.subtract(AUTHORISED_AMOUNT));
    }

    @Test
    void shouldMoveFundsOnSettlementAndClearTheHold() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.debitsPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.debitsPosted()).isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE.subtract(AUTHORISED_AMOUNT));
    }

    @Test
    void shouldReturnFundsOnReleaseWithoutSettlingAnything() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.release(clientTransactionId));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.debitsPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.debitsPosted()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE);
    }

    @Test
    void shouldHoldIncomingFundsOnInboundAuthorisationWithoutMakingThemSpendable() {
        UUID customerAccountId = givenFundedWallet();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.inboundAuthorisation(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.creditsPending()).isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE);
    }

    @Test
    void shouldMakeIncomingFundsSpendableOnSettlement() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.inboundAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.creditsPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE.add(AUTHORISED_AMOUNT));
    }

    @Test
    void shouldMoveFundsImmediatelyOnOutboundHardSettlement() {
        UUID customerAccountId = givenFundedWallet();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.outboundHardSettlement(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, customerAccountId, null));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.debitsPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.debitsPosted()).isEqualByComparingTo(AUTHORISED_AMOUNT);
    }

    @Test
    void shouldReuseTheDerivedTransferIdWhenAnAuthorisationIsRedelivered() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();
        LedgerPostingInstruction instruction =
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT);

        LedgerTransfer first = ledgerPostingService.applyPostingInstruction(instruction).getFirst();
        LedgerTransfer redelivered = ledgerPostingService.applyPostingInstruction(instruction).getFirst();

        assertThat(redelivered.id())
                .isEqualTo(first.id())
                .isEqualTo(LedgerTransferIds.deriveTransferId(clientTransactionId, OUTBOUND_AUTHORISATION));
        assertThat(walletOf(customerAccountId).debitsPending()).isEqualByComparingTo(AUTHORISED_AMOUNT);
    }

    @Test
    void shouldNotSettleTwiceWhenSettlementIsRedelivered() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        assertThat(walletOf(customerAccountId).debitsPosted()).isEqualByComparingTo(AUTHORISED_AMOUNT);
    }

    @Test
    void shouldNotReleaseTwiceWhenReleaseIsRedelivered() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.release(clientTransactionId));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.release(clientTransactionId));

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.debitsPending()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE);
    }

    @Test
    void shouldRejectAuthorisationThatExceedsAvailableFunds() {
        UUID customerAccountId = givenFundedWallet();
        LedgerPostingInstruction instruction =
                outboundAuthorisation(UUID.randomUUID(), customerAccountId, MORE_THAN_THE_BALANCE);

        assertThatThrownBy(() -> ledgerPostingService.applyPostingInstruction(instruction))
                .isInstanceOf(InsufficientLedgerFundsException.class);
    }

    @Test
    void shouldLeaveTheBalanceUntouchedWhenAuthorisationIsRejected() {
        UUID customerAccountId = givenFundedWallet();
        LedgerPostingInstruction instruction =
                outboundAuthorisation(UUID.randomUUID(), customerAccountId, MORE_THAN_THE_BALANCE);

        assertThatThrownBy(() -> ledgerPostingService.applyPostingInstruction(instruction))
                .isInstanceOf(InsufficientLedgerFundsException.class);

        LedgerAccount wallet = walletOf(customerAccountId);
        assertThat(wallet.availableBalance()).isEqualByComparingTo(OPENING_BALANCE);
        assertThat(wallet.debitsPending()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectSettlementWithoutAnAuthorisation() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.settlement(UUID.randomUUID());

        assertThatThrownBy(() -> ledgerPostingService.applyPostingInstruction(instruction))
                .isInstanceOf(PendingAuthorisationNotFoundException.class);
    }

    @Test
    void shouldRejectReleaseWithoutAnAuthorisation() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.release(UUID.randomUUID());

        assertThatThrownBy(() -> ledgerPostingService.applyPostingInstruction(instruction))
                .isInstanceOf(PendingAuthorisationNotFoundException.class);
    }

    @Test
    void shouldRejectACustomerAccountTypeAsTheInternalCounterparty() {
        UUID customerAccountId = givenFundedWallet();
        LedgerPostingInstruction instruction = LedgerPostingInstruction.outboundAuthorisation(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, customerAccountId, LedgerAccountType.WALLET);

        assertThatThrownBy(() -> ledgerPostingService.applyPostingInstruction(instruction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an internal account type");
    }

    @Test
    void shouldPostAgainstTheRequestedInternalAccount() {
        UUID customerAccountId = givenFundedWallet();
        BigDecimal feesBefore = internalAccountBalance(LedgerAccountType.FEES_INCOME);

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.outboundHardSettlement(
                UUID.randomUUID(), AUTHORISED_AMOUNT, CURRENCY, customerAccountId, LedgerAccountType.FEES_INCOME));

        assertThat(internalAccountBalance(LedgerAccountType.FEES_INCOME))
                .isEqualByComparingTo(feesBefore.add(AUTHORISED_AMOUNT));
    }

    @Test
    void shouldRecordEveryPostingAgainstTheClientTransaction() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        assertThat(ledgerPostingService.getPostingsByClientTransactionId(clientTransactionId))
                .extracting(LedgerTransfer::postingInstructionType)
                .containsExactlyInAnyOrder(OUTBOUND_AUTHORISATION, SETTLEMENT);
    }

    @Test
    void shouldReportTheSettledAmountRatherThanTheAmountMaxSentToTigerBeetle() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        LedgerTransfer settlement = ledgerPostingService.applyPostingInstruction(
                LedgerPostingInstruction.settlement(clientTransactionId)).getFirst();

        assertThat(settlement.amount()).isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(settlement.pendingTransferId())
                .isEqualTo(LedgerTransferIds.deriveTransferId(clientTransactionId, OUTBOUND_AUTHORISATION));
    }

    @Test
    void shouldKeepEveryPostingBalancedAcrossAFullAuthoriseAndSettleCycle() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        BigDecimal walletNetBefore = walletNet(customerAccountId);
        BigDecimal clearingNetBefore = internalAccountBalance(LedgerAccountType.OUTBOUND_CLEARING);

        ledgerPostingService.applyPostingInstruction(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        BigDecimal walletDelta = walletNet(customerAccountId).subtract(walletNetBefore);
        BigDecimal clearingDelta = internalAccountBalance(LedgerAccountType.OUTBOUND_CLEARING)
                .subtract(clearingNetBefore);

        assertThat(walletDelta).isEqualByComparingTo(AUTHORISED_AMOUNT.negate());
        assertThat(walletDelta.add(clearingDelta)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private UUID givenFundedWallet() {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, CURRENCY);
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.inboundHardSettlement(
                UUID.randomUUID(), OPENING_BALANCE, CURRENCY, customerAccountId, null));
        return customerAccountId;
    }

    private LedgerAccount walletOf(UUID customerAccountId) {
        return ledgerAccountService.getWallet(customerAccountId, CURRENCY);
    }

    private BigDecimal internalAccountBalance(LedgerAccountType internalAccountType) {
        return ledgerInternalAccountService.getInternalAccount(internalAccountType, CURRENCY).netBalance();
    }

    private BigDecimal walletNet(UUID customerAccountId) {
        LedgerAccount wallet = walletOf(customerAccountId);
        return wallet.creditsPosted().subtract(wallet.debitsPosted());
    }

    private static LedgerPostingInstruction outboundAuthorisation(UUID clientTransactionId, UUID customerAccountId,
                                                                  BigDecimal amount) {
        return LedgerPostingInstruction.outboundAuthorisation(
                clientTransactionId, amount, CURRENCY, customerAccountId, null);
    }

    @Test
    void shouldHoldBothLegsOfACrossCurrencyTransfer() {
        UUID payerAccountId = givenFundedWallet();
        UUID payeeAccountId = givenWalletIn(Currency.EUR);
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.crossCurrencyTransferAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY,
                new BigDecimal("290.00"), Currency.EUR, payerAccountId, payeeAccountId));

        LedgerAccount payerWallet = ledgerAccountService.getWallet(payerAccountId, CURRENCY);
        LedgerAccount payeeWallet = ledgerAccountService.getWallet(payeeAccountId, Currency.EUR);

        assertThat(payerWallet.debitsPending()).isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(payeeWallet.creditsPending()).isEqualByComparingTo(new BigDecimal("290.00"));
    }

    @Test
    void shouldSettleBothLegsOfACrossCurrencyTransfer() {
        UUID payerAccountId = givenFundedWallet();
        UUID payeeAccountId = givenWalletIn(Currency.EUR);
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.crossCurrencyTransferAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY,
                new BigDecimal("290.00"), Currency.EUR, payerAccountId, payeeAccountId));

        List<LedgerTransfer> settlements = ledgerPostingService.applyPostingInstruction(
                LedgerPostingInstruction.settlement(clientTransactionId));

        assertThat(settlements).hasSize(2);
        assertThat(ledgerAccountService.getWallet(payerAccountId, CURRENCY).debitsPosted())
                .isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(ledgerAccountService.getWallet(payeeAccountId, Currency.EUR).availableBalance())
                .isEqualByComparingTo(new BigDecimal("290.00"));
    }

    @Test
    void shouldLeaveTheBankExposureOnTheFxPositionAccounts() {
        UUID payerAccountId = givenFundedWallet();
        UUID payeeAccountId = givenWalletIn(Currency.EUR);
        UUID clientTransactionId = UUID.randomUUID();

        BigDecimal sellPositionBefore = fxPositionOf(CURRENCY);
        BigDecimal buyPositionBefore = fxPositionOf(Currency.EUR);

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.crossCurrencyTransferAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY,
                new BigDecimal("290.00"), Currency.EUR, payerAccountId, payeeAccountId));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.settlement(clientTransactionId));

        assertThat(fxPositionOf(CURRENCY).subtract(sellPositionBefore))
                .isEqualByComparingTo(AUTHORISED_AMOUNT);
        assertThat(fxPositionOf(Currency.EUR).subtract(buyPositionBefore))
                .isEqualByComparingTo(new BigDecimal("-290.00"));
    }

    @Test
    void shouldReleaseBothLegsOfACrossCurrencyTransfer() {
        UUID payerAccountId = givenFundedWallet();
        UUID payeeAccountId = givenWalletIn(Currency.EUR);
        UUID clientTransactionId = UUID.randomUUID();

        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.crossCurrencyTransferAuthorisation(
                clientTransactionId, AUTHORISED_AMOUNT, CURRENCY,
                new BigDecimal("290.00"), Currency.EUR, payerAccountId, payeeAccountId));
        ledgerPostingService.applyPostingInstruction(LedgerPostingInstruction.release(clientTransactionId));

        assertThat(ledgerAccountService.getWallet(payerAccountId, CURRENCY).availableBalance())
                .isEqualByComparingTo(OPENING_BALANCE);
        assertThat(ledgerAccountService.getWallet(payeeAccountId, Currency.EUR).creditsPending())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private UUID givenWalletIn(Currency currency) {
        UUID customerAccountId = UUID.randomUUID();
        ledgerAccountService.createLedgerAccount(customerAccountId, currency);
        return customerAccountId;
    }

    private BigDecimal fxPositionOf(Currency currency) {
        return ledgerInternalAccountService
                .getInternalAccount(LedgerAccountType.FX_POSITION, currency)
                .netBalance();
    }
}
