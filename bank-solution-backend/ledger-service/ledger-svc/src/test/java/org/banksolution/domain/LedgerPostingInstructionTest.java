package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.enums.PostingInstructionType.*;

class LedgerPostingInstructionTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    @Test
    void shouldBuildInboundAuthorisation() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.inboundAuthorisation(
                CLIENT_TRANSACTION_ID, AMOUNT, Currency.GBP, CUSTOMER_ACCOUNT_ID, null);

        assertThat(instruction.postingInstructionType()).isEqualTo(INBOUND_AUTHORISATION);
        assertThat(instruction.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(instruction.currency()).isEqualTo(Currency.GBP);
        assertThat(instruction.customerAccountId()).isEqualTo(CUSTOMER_ACCOUNT_ID);
    }

    @Test
    void shouldBuildOutboundAuthorisation() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.outboundAuthorisation(
                CLIENT_TRANSACTION_ID, AMOUNT, Currency.GBP, CUSTOMER_ACCOUNT_ID, null);

        assertThat(instruction.postingInstructionType()).isEqualTo(OUTBOUND_AUTHORISATION);
    }

    @Test
    void shouldBuildInboundHardSettlement() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.inboundHardSettlement(
                CLIENT_TRANSACTION_ID, AMOUNT, Currency.GBP, CUSTOMER_ACCOUNT_ID, null);

        assertThat(instruction.postingInstructionType()).isEqualTo(INBOUND_HARD_SETTLEMENT);
    }

    @Test
    void shouldBuildOutboundHardSettlement() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.outboundHardSettlement(
                CLIENT_TRANSACTION_ID, AMOUNT, Currency.GBP, CUSTOMER_ACCOUNT_ID, null);

        assertThat(instruction.postingInstructionType()).isEqualTo(OUTBOUND_HARD_SETTLEMENT);
    }

    @Test
    void shouldCarryAnExplicitInternalAccountTypeWhenGiven() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.outboundHardSettlement(
                CLIENT_TRANSACTION_ID, AMOUNT, Currency.GBP, CUSTOMER_ACCOUNT_ID, LedgerAccountType.FEES_INCOME);

        assertThat(instruction.internalAccountType()).isEqualTo(LedgerAccountType.FEES_INCOME);
    }

    @Test
    void shouldBuildSettlementCarryingOnlyTheClientTransaction() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.settlement(CLIENT_TRANSACTION_ID);

        assertThat(instruction.postingInstructionType()).isEqualTo(SETTLEMENT);
        assertThat(instruction.clientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID);
        assertThat(instruction.amount()).isNull();
        assertThat(instruction.currency()).isNull();
        assertThat(instruction.customerAccountId()).isNull();
        assertThat(instruction.internalAccountType()).isNull();
    }

    @Test
    void shouldBuildReleaseCarryingOnlyTheClientTransaction() {
        LedgerPostingInstruction instruction = LedgerPostingInstruction.release(CLIENT_TRANSACTION_ID);

        assertThat(instruction.postingInstructionType()).isEqualTo(RELEASE);
        assertThat(instruction.amount()).isNull();
        assertThat(instruction.customerAccountId()).isNull();
    }
}
