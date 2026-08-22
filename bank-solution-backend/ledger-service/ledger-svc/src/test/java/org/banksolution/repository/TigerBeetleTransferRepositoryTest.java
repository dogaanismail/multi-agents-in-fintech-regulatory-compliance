package org.banksolution.repository;

import org.banksolution.common.BaseIntegrationTest;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;
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

class TigerBeetleTransferRepositoryTest extends BaseIntegrationTest {

    private static final Currency CURRENCY = Currency.GBP;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal AUTHORISED_AMOUNT = new BigDecimal("250.00");

    @Autowired
    private TigerBeetleTransferRepository tigerBeetleTransferRepository;

    @Autowired
    private TigerBeetleAccountRepository tigerBeetleAccountRepository;

    @Test
    void shouldPersistAndRetrieveASinglePhaseTransfer() {
        UUID customerAccountId = givenWallet();
        UUID clientTransactionId = UUID.randomUUID();

        LedgerTransfer persisted = tigerBeetleTransferRepository.persistLedgerTransfer(
                inboundHardSettlement(clientTransactionId, customerAccountId, OPENING_BALANCE));

        assertThat(persisted.id())
                .isEqualTo(LedgerTransferIds.deriveTransferId(clientTransactionId, INBOUND_HARD_SETTLEMENT));
        assertThat(persisted.clientTransactionId()).isEqualTo(clientTransactionId);
        assertThat(persisted.postingInstructionType()).isEqualTo(INBOUND_HARD_SETTLEMENT);
        assertThat(persisted.amount()).isEqualByComparingTo(OPENING_BALANCE);
        assertThat(persisted.currency()).isEqualTo(CURRENCY);
        assertThat(persisted.createdAt()).isNotNull();
    }

    @Test
    void shouldPersistAPendingTransferCarryingItsTimeout() {
        UUID customerAccountId = givenFundedWallet();

        LedgerTransfer persisted = tigerBeetleTransferRepository.persistLedgerTransfer(
                outboundAuthorisation(UUID.randomUUID(), customerAccountId, AUTHORISED_AMOUNT));

        assertThat(persisted.postingInstructionType()).isEqualTo(OUTBOUND_AUTHORISATION);
        assertThat(persisted.timeoutSeconds()).isPositive();
    }

    @Test
    void shouldTreatARedeliveredTransferAsAlreadyApplied() {
        UUID customerAccountId = givenFundedWallet();
        LedgerTransfer transfer = outboundAuthorisation(UUID.randomUUID(), customerAccountId, AUTHORISED_AMOUNT);

        LedgerTransfer first = tigerBeetleTransferRepository.persistLedgerTransfer(transfer);
        LedgerTransfer redelivered = tigerBeetleTransferRepository.persistLedgerTransfer(transfer);

        assertThat(redelivered.id()).isEqualTo(first.id());
        assertThat(redelivered.createdAt()).isEqualTo(first.createdAt());
    }

    @Test
    void shouldRejectATransferThatWouldOverdrawTheWallet() {
        UUID customerAccountId = givenFundedWallet();
        LedgerTransfer transfer =
                outboundAuthorisation(UUID.randomUUID(), customerAccountId, new BigDecimal("100000.00"));

        assertThatThrownBy(() -> tigerBeetleTransferRepository.persistLedgerTransfer(transfer))
                .isInstanceOf(InsufficientLedgerFundsException.class);
    }

    @Test
    void shouldRejectAPostAgainstAnAuthorisationThatDoesNotExist() {
        UUID clientTransactionId = UUID.randomUUID();
        LedgerTransfer settlement = LedgerTransfer.builder()
                .id(LedgerTransferIds.deriveTransferId(clientTransactionId, SETTLEMENT))
                .clientTransactionId(clientTransactionId)
                .postingInstructionType(SETTLEMENT)
                .pendingTransferId(UUID.randomUUID())
                .build();

        assertThatThrownBy(() -> tigerBeetleTransferRepository.persistLedgerTransfer(settlement))
                .isInstanceOf(PendingAuthorisationNotFoundException.class);
    }

    @Test
    void shouldReturnEmptyWhenTheTransferDoesNotExist() {
        assertThat(tigerBeetleTransferRepository.findLedgerTransferById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoTransferIdsRequested() {
        assertThat(tigerBeetleTransferRepository.findLedgerTransfersByIds(List.of())).isEmpty();
    }

    @Test
    void shouldReturnOnlyTheTransfersThatExistForTheRequestedIds() {
        UUID customerAccountId = givenWallet();
        UUID clientTransactionId = UUID.randomUUID();

        tigerBeetleTransferRepository.persistLedgerTransfer(
                inboundHardSettlement(clientTransactionId, customerAccountId, OPENING_BALANCE));

        List<UUID> requestedIds = List.of(
                LedgerTransferIds.deriveTransferId(clientTransactionId, INBOUND_HARD_SETTLEMENT),
                LedgerTransferIds.deriveTransferId(clientTransactionId, OUTBOUND_AUTHORISATION));

        assertThat(tigerBeetleTransferRepository.findLedgerTransfersByIds(requestedIds))
                .extracting(LedgerTransfer::postingInstructionType)
                .containsExactly(INBOUND_HARD_SETTLEMENT);
    }

    @Test
    void shouldFindEveryTransferRecordedAgainstTheClientTransaction() {
        UUID customerAccountId = givenFundedWallet();
        UUID clientTransactionId = UUID.randomUUID();

        tigerBeetleTransferRepository.persistLedgerTransfer(
                outboundAuthorisation(clientTransactionId, customerAccountId, AUTHORISED_AMOUNT));

        assertThat(tigerBeetleTransferRepository.findLedgerTransfersByClientTransactionId(clientTransactionId))
                .extracting(LedgerTransfer::postingInstructionType)
                .containsExactly(OUTBOUND_AUTHORISATION);
    }

    @Test
    void shouldReturnNoPostingsForAnUnknownClientTransaction() {
        assertThat(tigerBeetleTransferRepository
                .findLedgerTransfersByClientTransactionId(UUID.randomUUID()))
                .isEmpty();
    }

    private UUID givenWallet() {
        UUID customerAccountId = UUID.randomUUID();
        tigerBeetleAccountRepository.persistLedgerAccount(LedgerAccount.newWallet(customerAccountId, CURRENCY));
        return customerAccountId;
    }

    private UUID givenFundedWallet() {
        UUID customerAccountId = givenWallet();
        tigerBeetleTransferRepository.persistLedgerTransfer(
                inboundHardSettlement(UUID.randomUUID(), customerAccountId, OPENING_BALANCE));
        return customerAccountId;
    }

    private static LedgerTransfer inboundHardSettlement(
            UUID clientTransactionId,
            UUID customerAccountId,
            BigDecimal amount) {

        return movementTransfer(
                clientTransactionId,
                INBOUND_HARD_SETTLEMENT,
                LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.INBOUND_CLEARING, CURRENCY),
                LedgerAccountIds.deriveWalletAccountId(customerAccountId, CURRENCY),
                amount);
    }

    private static LedgerTransfer outboundAuthorisation(
            UUID clientTransactionId,
            UUID customerAccountId,
            BigDecimal amount) {

        return movementTransfer(
                clientTransactionId,
                OUTBOUND_AUTHORISATION,
                LedgerAccountIds.deriveWalletAccountId(customerAccountId, CURRENCY),
                LedgerAccountIds.deriveInternalAccountId(LedgerAccountType.OUTBOUND_CLEARING, CURRENCY),
                amount);
    }

    private static LedgerTransfer movementTransfer(
            UUID clientTransactionId,
            PostingInstructionType postingInstructionType,
            UUID debitAccountId,
            UUID creditAccountId,
            BigDecimal amount) {

        return LedgerTransfer.builder()
                .id(LedgerTransferIds.deriveTransferId(clientTransactionId, postingInstructionType))
                .clientTransactionId(clientTransactionId)
                .postingInstructionType(postingInstructionType)
                .debitAccountId(debitAccountId)
                .creditAccountId(creditAccountId)
                .amount(amount)
                .currency(CURRENCY)
                .timeoutSeconds(postingInstructionType.isAuthorisation() ? 900 : 0)
                .build();
    }
}
