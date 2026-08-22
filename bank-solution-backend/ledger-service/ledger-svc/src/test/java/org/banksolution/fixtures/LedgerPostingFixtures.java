package org.banksolution.fixtures;

import org.banksolution.enums.Currency;
import org.banksolution.model.request.CreateLedgerPostingInstructionRequest;
import org.banksolution.model.request.CreateLedgerPostingInstructionRequest.ReleaseRequest;
import org.banksolution.model.request.CreateLedgerPostingInstructionRequest.SettlementRequest;
import org.banksolution.model.request.CustomerAccountMovementRequest;

import java.math.BigDecimal;
import java.util.UUID;

public final class LedgerPostingFixtures {

    private LedgerPostingFixtures() {
    }

    public static CustomerAccountMovementRequest createCustomerAccountMovement(UUID accountId, BigDecimal amount, Currency currency) {
        return CustomerAccountMovementRequest.builder()
                .customerAccountId(accountId)
                .amount(amount)
                .currency(currency)
                .build();
    }

    public static CreateLedgerPostingInstructionRequest createInboundHardSettlement(
            UUID clientTransactionId, UUID accountId, BigDecimal amount, Currency currency) {
        return CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(clientTransactionId)
                .inboundHardSettlement(createCustomerAccountMovement(accountId, amount, currency))
                .build();
    }

    public static CreateLedgerPostingInstructionRequest createOutboundHardSettlement(
            UUID clientTransactionId, UUID accountId, BigDecimal amount, Currency currency) {
        return CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(clientTransactionId)
                .outboundHardSettlement(createCustomerAccountMovement(accountId, amount, currency))
                .build();
    }

    public static CreateLedgerPostingInstructionRequest createInboundAuthorisation(
            UUID clientTransactionId, UUID accountId, BigDecimal amount, Currency currency) {
        return CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(clientTransactionId)
                .inboundAuthorisation(createCustomerAccountMovement(accountId, amount, currency))
                .build();
    }

    public static CreateLedgerPostingInstructionRequest createOutboundAuthorisation(
            UUID clientTransactionId, UUID accountId, BigDecimal amount, Currency currency) {
        return CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(clientTransactionId)
                .outboundAuthorisation(createCustomerAccountMovement(accountId, amount, currency))
                .build();
    }

    public static CreateLedgerPostingInstructionRequest createSettlement(UUID clientTransactionId) {
        return CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(clientTransactionId)
                .settlement(new SettlementRequest())
                .build();
    }

    public static CreateLedgerPostingInstructionRequest createRelease(UUID clientTransactionId) {
        return CreateLedgerPostingInstructionRequest.builder()
                .clientTransactionId(clientTransactionId)
                .release(new ReleaseRequest())
                .build();
    }
}
