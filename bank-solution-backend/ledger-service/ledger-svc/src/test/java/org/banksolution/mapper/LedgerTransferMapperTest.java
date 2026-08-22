package org.banksolution.mapper;

import com.tigerbeetle.TransferBatch;
import com.tigerbeetle.UInt128;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.enums.Currency;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.model.response.LedgerPostingResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.SETTLEMENT;

class LedgerTransferMapperTest {

    private static final UUID TRANSFER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID CLIENT_TRANSACTION_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID DEBIT_ACCOUNT_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final UUID CREDIT_ACCOUNT_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");
    private static final UUID PENDING_TRANSFER_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");

    private static final long GBP_MINOR_UNITS = 100_000L;
    private static final int AUTHORISATION_TIMEOUT_SECONDS = 900;
    private static final long TIMESTAMP_NANOS = 1_700_000_000_123_456_789L;

    @Test
    void shouldMapEveryFieldFromTheTigerBeetleTransfer() {
        TransferBatch transferBatch = createTransferBatch(
                OUTBOUND_AUTHORISATION,
                Currency.GBP,
                GBP_MINOR_UNITS);

        LedgerTransfer ledgerTransfer = LedgerTransferMapper.toLedgerTransfer(transferBatch);

        assertThat(ledgerTransfer.id()).isEqualTo(TRANSFER_ID);
        assertThat(ledgerTransfer.clientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID);
        assertThat(ledgerTransfer.postingInstructionType()).isEqualTo(OUTBOUND_AUTHORISATION);
        assertThat(ledgerTransfer.debitAccountId()).isEqualTo(DEBIT_ACCOUNT_ID);
        assertThat(ledgerTransfer.creditAccountId()).isEqualTo(CREDIT_ACCOUNT_ID);
        assertThat(ledgerTransfer.currency()).isEqualTo(Currency.GBP);
        assertThat(ledgerTransfer.timeoutSeconds()).isEqualTo(AUTHORISATION_TIMEOUT_SECONDS);
    }

    @Test
    void shouldReadTheCurrencyFromTheTigerBeetleLedger() {
        TransferBatch transferBatch = createTransferBatch(OUTBOUND_AUTHORISATION, Currency.JPY, 1_000L);

        assertThat(LedgerTransferMapper.toLedgerTransfer(transferBatch).currency()).isEqualTo(Currency.JPY);
    }

    @Test
    void shouldConvertMinorUnitsUsingTheCurrencyExponent() {
        TransferBatch gbpBatch = createTransferBatch(OUTBOUND_AUTHORISATION, Currency.GBP, GBP_MINOR_UNITS);

        assertThat(LedgerTransferMapper.toLedgerTransfer(gbpBatch).amount())
                .isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void shouldNotAddDecimalsToAZeroExponentCurrency() {
        TransferBatch jpyBatch = createTransferBatch(OUTBOUND_AUTHORISATION, Currency.JPY, 1_000L);

        assertThat(LedgerTransferMapper.toLedgerTransfer(jpyBatch).amount())
                .isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void shouldReadThePostingInstructionTypeFromUserData32() {
        TransferBatch settlementBatch = createTransferBatch(SETTLEMENT, Currency.GBP, GBP_MINOR_UNITS);

        assertThat(LedgerTransferMapper.toLedgerTransfer(settlementBatch).postingInstructionType())
                .isEqualTo(SETTLEMENT);
    }

    @Test
    void shouldMapThePendingTransferIdWhenTheTransferResolvesAnAuthorisation() {
        TransferBatch transferBatch = createTransferBatch(SETTLEMENT, Currency.GBP, GBP_MINOR_UNITS);
        transferBatch.setPendingId(UInt128.asBytes(PENDING_TRANSFER_ID));

        assertThat(LedgerTransferMapper.toLedgerTransfer(transferBatch).pendingTransferId())
                .isEqualTo(PENDING_TRANSFER_ID);
    }

    @Test
    void shouldLeaveThePendingTransferIdNullWhenTigerBeetleReportsZero() {
        TransferBatch transferBatch = createTransferBatch(OUTBOUND_AUTHORISATION, Currency.GBP, GBP_MINOR_UNITS);

        assertThat(LedgerTransferMapper.toLedgerTransfer(transferBatch).pendingTransferId()).isNull();
    }

    @Test
    void shouldConvertTheNanosecondTimestampToAnInstant() {
        TransferBatch transferBatch = createTransferBatch(OUTBOUND_AUTHORISATION, Currency.GBP, GBP_MINOR_UNITS);
        transferBatch.setTimestamp(TIMESTAMP_NANOS);

        assertThat(LedgerTransferMapper.toLedgerTransfer(transferBatch).createdAt())
                .isEqualTo(Instant.ofEpochSecond(1_700_000_000L, 123_456_789L));
    }

    @Test
    void shouldLeaveCreatedAtNullForAnUntimestampedTransfer() {
        TransferBatch transferBatch = createTransferBatch(OUTBOUND_AUTHORISATION, Currency.GBP, GBP_MINOR_UNITS);

        assertThat(LedgerTransferMapper.toLedgerTransfer(transferBatch).createdAt()).isNull();
    }

    @Test
    void shouldMapTheLedgerTransferOntoThePostingResponse() {
        LedgerTransfer ledgerTransfer = LedgerTransfer.builder()
                .id(TRANSFER_ID)
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .postingInstructionType(SETTLEMENT)
                .debitAccountId(DEBIT_ACCOUNT_ID)
                .creditAccountId(CREDIT_ACCOUNT_ID)
                .amount(new BigDecimal("250.00"))
                .currency(Currency.GBP)
                .pendingTransferId(PENDING_TRANSFER_ID)
                .createdAt(Instant.EPOCH)
                .build();

        LedgerPostingResponse response = LedgerTransferMapper.toLedgerPostingResponse(ledgerTransfer);

        assertThat(response.getTransferId()).isEqualTo(TRANSFER_ID);
        assertThat(response.getClientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID);
        assertThat(response.getPostingInstructionType()).isEqualTo(SETTLEMENT);
        assertThat(response.getDebitAccountId()).isEqualTo(DEBIT_ACCOUNT_ID);
        assertThat(response.getCreditAccountId()).isEqualTo(CREDIT_ACCOUNT_ID);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(response.getCurrency()).isEqualTo(Currency.GBP);
        assertThat(response.getPendingTransferId()).isEqualTo(PENDING_TRANSFER_ID);
        assertThat(response.getCreatedAt()).isEqualTo(Instant.EPOCH);
    }

    private static TransferBatch createTransferBatch(
            PostingInstructionType postingInstructionType,
            Currency currency,
            long minorUnits) {

        TransferBatch transferBatch = new TransferBatch(1);
        transferBatch.add();
        transferBatch.setId(UInt128.asBytes(TRANSFER_ID));
        transferBatch.setUserData128(UInt128.asBytes(CLIENT_TRANSACTION_ID));
        transferBatch.setUserData32(postingInstructionType.getCode());
        transferBatch.setDebitAccountId(UInt128.asBytes(DEBIT_ACCOUNT_ID));
        transferBatch.setCreditAccountId(UInt128.asBytes(CREDIT_ACCOUNT_ID));
        transferBatch.setAmount(minorUnits);
        transferBatch.setLedger(currency.getNumericCode());
        transferBatch.setCode(postingInstructionType.getCode());
        transferBatch.setTimeout(AUTHORISATION_TIMEOUT_SECONDS);

        return transferBatch;
    }
}
