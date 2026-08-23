package org.banksolution.mapper;

import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.enums.PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.RELEASE;
import static org.banksolution.enums.PostingInstructionType.SETTLEMENT;
import static org.banksolution.mapper.LedgerPostingTransferMapper.toAuthorisationFollowUpLedgerTransfers;
import static org.banksolution.mapper.LedgerPostingTransferMapper.toMovementLedgerTransfers;

class LedgerPostingTransferMapperTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID PAYER_ACCOUNT_ID = UUID.randomUUID();
    private static final UUID PAYEE_ACCOUNT_ID = UUID.randomUUID();
    private static final BigDecimal SELL_AMOUNT = new BigDecimal("250.00");
    private static final BigDecimal BUY_AMOUNT = new BigDecimal("290.00");
    private static final int TIMEOUT_SECONDS = 120;

    @Test
    void shouldDebitTheWalletAndCreditOutboundClearingOnAnOutboundAuthorisation() {
        LedgerTransfer transfer = toMovementLedgerTransfers(
                LedgerPostingInstruction.outboundAuthorisation(
                        CLIENT_TRANSACTION_ID, SELL_AMOUNT, Currency.GBP, PAYER_ACCOUNT_ID, null),
                TIMEOUT_SECONDS).getFirst();

        assertThat(transfer.debitAccountId())
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(PAYER_ACCOUNT_ID, Currency.GBP));
        assertThat(transfer.creditAccountId())
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(
                        LedgerAccountType.OUTBOUND_CLEARING, Currency.GBP));
        assertThat(transfer.timeoutSeconds()).isEqualTo(TIMEOUT_SECONDS);
    }

    @Test
    void shouldDebitInboundClearingAndCreditTheWalletOnAnInboundAuthorisation() {
        LedgerTransfer transfer = toMovementLedgerTransfers(
                LedgerPostingInstruction.inboundAuthorisation(
                        CLIENT_TRANSACTION_ID, SELL_AMOUNT, Currency.GBP, PAYEE_ACCOUNT_ID, null),
                TIMEOUT_SECONDS).getFirst();

        assertThat(transfer.debitAccountId())
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(
                        LedgerAccountType.INBOUND_CLEARING, Currency.GBP));
        assertThat(transfer.creditAccountId())
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(PAYEE_ACCOUNT_ID, Currency.GBP));
    }

    @Test
    void shouldMoveDirectlyBetweenWalletsOnAnInternalTransferAuthorisation() {
        LedgerTransfer transfer = toMovementLedgerTransfers(
                LedgerPostingInstruction.internalTransferAuthorisation(
                        CLIENT_TRANSACTION_ID, SELL_AMOUNT, Currency.GBP, PAYER_ACCOUNT_ID, PAYEE_ACCOUNT_ID),
                TIMEOUT_SECONDS).getFirst();

        assertThat(transfer.debitAccountId())
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(PAYER_ACCOUNT_ID, Currency.GBP));
        assertThat(transfer.creditAccountId())
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(PAYEE_ACCOUNT_ID, Currency.GBP));
    }

    @Test
    void shouldNotApplyATimeoutToAHardSettlement() {
        LedgerTransfer transfer = toMovementLedgerTransfers(
                LedgerPostingInstruction.outboundHardSettlement(
                        CLIENT_TRANSACTION_ID, SELL_AMOUNT, Currency.GBP, PAYER_ACCOUNT_ID, null),
                TIMEOUT_SECONDS).getFirst();

        assertThat(transfer.timeoutSeconds()).isZero();
    }

    @Test
    void shouldBridgeTheTwoCurrenciesThroughTheFxPositionAccounts() {
        List<LedgerTransfer> transfers = toMovementLedgerTransfers(crossCurrencyAuthorisation(), TIMEOUT_SECONDS);

        LedgerTransfer sellLeg = transfers.getFirst();
        assertThat(sellLeg.debitAccountId())
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(PAYER_ACCOUNT_ID, Currency.GBP));
        assertThat(sellLeg.creditAccountId())
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.FX_POSITION, Currency.GBP));
        assertThat(sellLeg.amount()).isEqualByComparingTo(SELL_AMOUNT);
        assertThat(sellLeg.currency()).isEqualTo(Currency.GBP);

        LedgerTransfer buyLeg = transfers.getLast();
        assertThat(buyLeg.debitAccountId())
                .isEqualTo(LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.FX_POSITION, Currency.EUR));
        assertThat(buyLeg.creditAccountId())
                .isEqualTo(LedgerAccountIds.deriveWalletAccountId(PAYEE_ACCOUNT_ID, Currency.EUR));
        assertThat(buyLeg.amount()).isEqualByComparingTo(BUY_AMOUNT);
        assertThat(buyLeg.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void shouldGiveEachCrossCurrencyLegItsOwnDerivedTransferId() {
        List<LedgerTransfer> transfers = toMovementLedgerTransfers(crossCurrencyAuthorisation(), TIMEOUT_SECONDS);

        assertThat(transfers).extracting(LedgerTransfer::id).doesNotHaveDuplicates();
        assertThat(transfers.getFirst().id()).isEqualTo(LedgerTransferIds.deriveTransferId(
                CLIENT_TRANSACTION_ID, CROSS_CURRENCY_TRANSFER_AUTHORISATION, 0));
        assertThat(transfers.getLast().id()).isEqualTo(LedgerTransferIds.deriveTransferId(
                CLIENT_TRANSACTION_ID, CROSS_CURRENCY_TRANSFER_AUTHORISATION, 1));
    }

    @Test
    void shouldDeriveTheSameTransferIdWhenAnInstructionIsRedelivered() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.outboundAuthorisation(
                CLIENT_TRANSACTION_ID, SELL_AMOUNT, Currency.GBP, PAYER_ACCOUNT_ID, null);

        assertThat(toMovementLedgerTransfers(instruction, TIMEOUT_SECONDS).getFirst().id())
                .isEqualTo(toMovementLedgerTransfers(instruction, TIMEOUT_SECONDS).getFirst().id())
                .isEqualTo(LedgerTransferIds.deriveTransferId(CLIENT_TRANSACTION_ID, OUTBOUND_AUTHORISATION));
    }

    @Test
    void shouldBuildOneSettlementPerAuthorisedLeg() {
        List<UUID> authorisationTransferIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        List<LedgerTransfer> settlements = toAuthorisationFollowUpLedgerTransfers(
                LedgerPostingInstruction.settlement(CLIENT_TRANSACTION_ID), authorisationTransferIds);

        assertThat(settlements).hasSize(2);
        assertThat(settlements).extracting(LedgerTransfer::pendingTransferId)
                .containsExactlyElementsOf(authorisationTransferIds);
        assertThat(settlements).extracting(LedgerTransfer::postingInstructionType)
                .containsOnly(SETTLEMENT);
        assertThat(settlements).extracting(LedgerTransfer::id).doesNotHaveDuplicates();
    }

    @Test
    void shouldBuildOneReleasePerAuthorisedLeg() {
        List<UUID> authorisationTransferIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        List<LedgerTransfer> releases = toAuthorisationFollowUpLedgerTransfers(
                LedgerPostingInstruction.release(CLIENT_TRANSACTION_ID), authorisationTransferIds);

        assertThat(releases).extracting(LedgerTransfer::postingInstructionType).containsOnly(RELEASE);
        assertThat(releases).extracting(LedgerTransfer::pendingTransferId)
                .containsExactlyElementsOf(authorisationTransferIds);
    }

    @Test
    void shouldRejectACustomerAccountTypeAsTheInternalCounterparty() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.outboundAuthorisation(
                CLIENT_TRANSACTION_ID, SELL_AMOUNT, Currency.GBP, PAYER_ACCOUNT_ID, LedgerAccountType.WALLET);

        assertThatThrownBy(() -> toMovementLedgerTransfers(instruction, TIMEOUT_SECONDS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an internal account type");
    }

    private static LedgerPostingInstruction crossCurrencyAuthorisation() {
        return LedgerPostingInstruction.crossCurrencyTransferAuthorisation(
                CLIENT_TRANSACTION_ID,
                SELL_AMOUNT,
                Currency.GBP,
                BUY_AMOUNT,
                Currency.EUR,
                PAYER_ACCOUNT_ID,
                PAYEE_ACCOUNT_ID);
    }
}
