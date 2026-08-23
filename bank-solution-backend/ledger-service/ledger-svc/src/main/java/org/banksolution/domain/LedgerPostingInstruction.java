package org.banksolution.domain;

import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.banksolution.enums.PostingInstructionType.*;

public record LedgerPostingInstruction(
        UUID clientTransactionId,
        PostingInstructionType postingInstructionType,
        BigDecimal amount,
        Currency currency,
        BigDecimal buyAmount,
        Currency buyCurrency,
        UUID customerAccountId,
        UUID counterpartyCustomerAccountId,
        LedgerAccountType internalAccountType) {

    public static LedgerPostingInstruction inboundAuthorisation(
            UUID clientTransactionId,
            BigDecimal amount,
            Currency currency,
            UUID customerAccountId,
            LedgerAccountType internalAccountType) {

        return new LedgerPostingInstruction(clientTransactionId,
                INBOUND_AUTHORISATION,
                amount,
                currency,
                null,
                null,
                customerAccountId,
                null,
                internalAccountType);
    }

    public static LedgerPostingInstruction outboundAuthorisation(
            UUID clientTransactionId,
            BigDecimal amount,
            Currency currency,
            UUID customerAccountId,
            LedgerAccountType internalAccountType) {

        return new LedgerPostingInstruction(clientTransactionId,
                OUTBOUND_AUTHORISATION,
                amount,
                currency,
                null,
                null,
                customerAccountId,
                null,
                internalAccountType);
    }

    public static LedgerPostingInstruction internalTransferAuthorisation(
            UUID clientTransactionId,
            BigDecimal amount,
            Currency currency,
            UUID sourceCustomerAccountId,
            UUID destinationCustomerAccountId) {

        return new LedgerPostingInstruction(clientTransactionId,
                INTERNAL_TRANSFER_AUTHORISATION,
                amount,
                currency,
                null,
                null,
                sourceCustomerAccountId,
                destinationCustomerAccountId,
                null);
    }

    public static LedgerPostingInstruction inboundHardSettlement(
            UUID clientTransactionId,
            BigDecimal amount,
            Currency currency,
            UUID customerAccountId,
            LedgerAccountType internalAccountType) {

        return new LedgerPostingInstruction(clientTransactionId,
                INBOUND_HARD_SETTLEMENT,
                amount,
                currency,
                null,
                null,
                customerAccountId,
                null,
                internalAccountType);
    }

    public static LedgerPostingInstruction outboundHardSettlement(
            UUID clientTransactionId,
            BigDecimal amount,
            Currency currency,
            UUID customerAccountId,
            LedgerAccountType internalAccountType) {

        return new LedgerPostingInstruction(clientTransactionId,
                OUTBOUND_HARD_SETTLEMENT,
                amount,
                currency,
                null,
                null,
                customerAccountId,
                null,
                internalAccountType);
    }

    public static LedgerPostingInstruction settlement(UUID clientTransactionId) {

        return new LedgerPostingInstruction(clientTransactionId,
                SETTLEMENT,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static LedgerPostingInstruction release(UUID clientTransactionId) {

        return new LedgerPostingInstruction(clientTransactionId,
                RELEASE,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static LedgerPostingInstruction crossCurrencyTransferAuthorisation(
            UUID clientTransactionId,
            BigDecimal sellAmount,
            Currency sellCurrency,
            BigDecimal buyAmount,
            Currency buyCurrency,
            UUID sourceCustomerAccountId,
            UUID destinationCustomerAccountId) {

        return new LedgerPostingInstruction(
                clientTransactionId,
                CROSS_CURRENCY_TRANSFER_AUTHORISATION,
                sellAmount,
                sellCurrency,
                buyAmount,
                buyCurrency,
                sourceCustomerAccountId,
                destinationCustomerAccountId,
                null);
    }
}
