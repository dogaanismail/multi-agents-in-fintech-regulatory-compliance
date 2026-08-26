package org.banksolution.repository;

import com.tigerbeetle.AccountBatch;
import com.tigerbeetle.Client;
import com.tigerbeetle.IdBatch;
import com.tigerbeetle.QueryFilter;
import com.tigerbeetle.TransferBatch;
import org.banksolution.domain.LedgerAccount;
import org.banksolution.domain.LedgerInternalAccount;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.exception.LedgerUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The blocking client throws InterruptedException on shutdown; every repository call must
 * surface that as LedgerUnavailableException and keep the thread's interrupt flag set.
 */
class TigerBeetleRepositoriesInterruptionTest {

    private final Client tigerBeetleClient = mock(Client.class);
    private final TigerBeetleAccountRepository tigerBeetleAccountRepository = new TigerBeetleAccountRepository(tigerBeetleClient);
    private final TigerBeetleInternalAccountRepository tigerBeetleInternalAccountRepository =
            new TigerBeetleInternalAccountRepository(tigerBeetleClient);
    private final TigerBeetleTransferRepository tigerBeetleTransferRepository = new TigerBeetleTransferRepository(tigerBeetleClient);

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void shouldSurfaceAnInterruptedAccountLookupAsUnavailable() throws Exception {
        when(tigerBeetleClient.lookupAccounts(any(IdBatch.class))).thenThrow(new InterruptedException("shutting down"));

        assertUnavailable(() -> tigerBeetleAccountRepository.findLedgerAccountById(UUID.randomUUID()));
        assertUnavailable(() -> tigerBeetleInternalAccountRepository.findInternalAccountById(UUID.randomUUID()));
    }

    @Test
    void shouldSurfaceAnInterruptedAccountCreationAsUnavailable() throws Exception {
        when(tigerBeetleClient.createAccounts(any(AccountBatch.class))).thenThrow(new InterruptedException("shutting down"));

        assertUnavailable(() -> tigerBeetleAccountRepository.persistLedgerAccount(LedgerAccount.newWallet(UUID.randomUUID(), Currency.GBP)));
        assertUnavailable(() -> tigerBeetleInternalAccountRepository.persistInternalAccount(
                LedgerInternalAccount.newInternalAccount(LedgerAccountType.SUSPENSE, Currency.GBP)));
    }

    @Test
    void shouldSurfaceAnInterruptedWalletQueryAsUnavailable() throws Exception {
        when(tigerBeetleClient.queryAccounts(any(QueryFilter.class))).thenThrow(new InterruptedException("shutting down"));

        assertUnavailable(() -> tigerBeetleAccountRepository.findWalletAccountsByCurrency(Currency.GBP));
    }

    @Test
    void shouldSurfaceInterruptedTransferCallsAsUnavailable() throws Exception {
        when(tigerBeetleClient.createTransfers(any(TransferBatch.class))).thenThrow(new InterruptedException("shutting down"));
        when(tigerBeetleClient.lookupTransfers(any(IdBatch.class))).thenThrow(new InterruptedException("shutting down"));
        when(tigerBeetleClient.queryTransfers(any(QueryFilter.class))).thenThrow(new InterruptedException("shutting down"));
        LedgerTransfer ledgerTransfer = LedgerTransfer.builder()
                .id(UUID.randomUUID())
                .clientTransactionId(UUID.randomUUID())
                .postingInstructionType(PostingInstructionType.OUTBOUND_HARD_SETTLEMENT)
                .debitAccountId(UUID.randomUUID())
                .creditAccountId(UUID.randomUUID())
                .amount(new BigDecimal("1.00"))
                .currency(Currency.GBP)
                .build();

        assertUnavailable(() -> tigerBeetleTransferRepository.persistLedgerTransfer(ledgerTransfer));
        assertUnavailable(() -> tigerBeetleTransferRepository.findLedgerTransferById(UUID.randomUUID()));
        assertUnavailable(() -> tigerBeetleTransferRepository.findLedgerTransfersByIds(List.of(UUID.randomUUID())));
        assertUnavailable(() -> tigerBeetleTransferRepository.findLedgerTransfersByClientTransactionId(UUID.randomUUID()));
    }

    private static void assertUnavailable(Runnable repositoryCall) {
        Thread.interrupted();

        assertThatThrownBy(repositoryCall::run)
                .isInstanceOf(LedgerUnavailableException.class)
                .hasMessageContaining("shutting down")
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
