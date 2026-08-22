package org.banksolution.mapper;

import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.model.request.CreateLedgerPostingInstructionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.enums.PostingInstructionType.*;
import static org.banksolution.fixtures.LedgerPostingFixtures.*;
import static org.banksolution.mapper.LedgerPostingInstructionMapper.toLedgerPostingInstruction;

class LedgerPostingInstructionMapperTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    @Test
    void shouldMapInboundAuthorisation() {
        LedgerPostingInstruction instruction = toLedgerPostingInstruction(
                createInboundAuthorisation(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP));

        assertThat(instruction.postingInstructionType()).isEqualTo(INBOUND_AUTHORISATION);
        assertThat(instruction.clientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID);
        assertThat(instruction.customerAccountId()).isEqualTo(CUSTOMER_ACCOUNT_ID);
        assertThat(instruction.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(instruction.currency()).isEqualTo(Currency.GBP);
    }

    @Test
    void shouldMapOutboundAuthorisation() {
        LedgerPostingInstruction instruction = toLedgerPostingInstruction(
                createOutboundAuthorisation(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.EUR));

        assertThat(instruction.postingInstructionType()).isEqualTo(OUTBOUND_AUTHORISATION);
        assertThat(instruction.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void shouldMapInboundHardSettlement() {
        LedgerPostingInstruction instruction = toLedgerPostingInstruction(
                createInboundHardSettlement(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP));

        assertThat(instruction.postingInstructionType()).isEqualTo(INBOUND_HARD_SETTLEMENT);
    }

    @Test
    void shouldMapOutboundHardSettlement() {
        LedgerPostingInstruction instruction = toLedgerPostingInstruction(
                createOutboundHardSettlement(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP));

        assertThat(instruction.postingInstructionType()).isEqualTo(OUTBOUND_HARD_SETTLEMENT);
    }

    @Test
    void shouldMapSettlement() {
        LedgerPostingInstruction instruction = toLedgerPostingInstruction(createSettlement(CLIENT_TRANSACTION_ID));

        assertThat(instruction.postingInstructionType()).isEqualTo(SETTLEMENT);
        assertThat(instruction.clientTransactionId()).isEqualTo(CLIENT_TRANSACTION_ID);
        assertThat(instruction.amount()).isNull();
    }

    @Test
    void shouldMapRelease() {
        LedgerPostingInstruction instruction = toLedgerPostingInstruction(createRelease(CLIENT_TRANSACTION_ID));

        assertThat(instruction.postingInstructionType()).isEqualTo(RELEASE);
        assertThat(instruction.amount()).isNull();
    }

    @Test
    void shouldCarryTheRequestedInternalAccountType() {
        CreateLedgerPostingInstructionRequest request =
                createOutboundAuthorisation(CLIENT_TRANSACTION_ID, CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP);
        request.getOutboundAuthorisation().setInternalAccountType(LedgerAccountType.FEES_INCOME);

        assertThat(toLedgerPostingInstruction(request).internalAccountType())
                .isEqualTo(LedgerAccountType.FEES_INCOME);
    }

    @Test
    void shouldRejectRequestWithoutAnyPostingInstruction() {
        CreateLedgerPostingInstructionRequest request = CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .build();

        assertThatThrownBy(() -> toLedgerPostingInstruction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one posting instruction type");
    }
}
