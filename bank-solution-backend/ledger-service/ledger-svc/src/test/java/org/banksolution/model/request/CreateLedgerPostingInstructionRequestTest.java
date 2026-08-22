package org.banksolution.model.request;

import org.banksolution.enums.Currency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.banksolution.fixtures.LedgerPostingFixtures.createCustomerAccountMovement;
import static org.banksolution.fixtures.LedgerPostingFixtures.createSettlement;

class CreateLedgerPostingInstructionRequestTest {

    private static final UUID CLIENT_TRANSACTION_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ACCOUNT_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("250.00");

    @Test
    void shouldAcceptExactlyOnePostingInstruction() {
        assertThat(createSettlement(CLIENT_TRANSACTION_ID).isExactlyOnePostingInstructionProvided()).isTrue();
    }

    @Test
    void shouldRejectRequestWithNoPostingInstruction() {
        CreateLedgerPostingInstructionRequest request = CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .build();

        assertThat(request.isExactlyOnePostingInstructionProvided()).isFalse();
    }

    @Test
    void shouldRejectRequestWithTwoPostingInstructions() {
        CreateLedgerPostingInstructionRequest request = CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .inboundAuthorisation(createCustomerAccountMovement(CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP))
                .outboundAuthorisation(createCustomerAccountMovement(CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP))
                .build();

        assertThat(request.isExactlyOnePostingInstructionProvided()).isFalse();
    }

    @Test
    void shouldRejectRequestMixingAMovementWithSettlement() {
        CreateLedgerPostingInstructionRequest request = CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(CLIENT_TRANSACTION_ID)
                .outboundAuthorisation(createCustomerAccountMovement(CUSTOMER_ACCOUNT_ID, AMOUNT, Currency.GBP))
                .settlement(new CreateLedgerPostingInstructionRequest.SettlementRequest())
                .build();

        assertThat(request.isExactlyOnePostingInstructionProvided()).isFalse();
    }
}
