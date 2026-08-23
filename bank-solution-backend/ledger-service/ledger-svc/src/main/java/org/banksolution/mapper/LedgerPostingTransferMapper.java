package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.banksolution.domain.LedgerTransferIds.SINGLE_LEG;

@UtilityClass
public class LedgerPostingTransferMapper {

    private static final int NO_TIMEOUT = 0;
    private static final int SELL_LEG = 0;
    private static final int BUY_LEG = 1;

    public static List<LedgerTransfer> toMovementLedgerTransfers(
            LedgerPostingInstruction postingInstruction,
            int authorisationTimeoutSeconds) {

        PostingInstructionType postingInstructionType = postingInstruction.postingInstructionType();

        if (postingInstructionType.crossesCurrencies()) {
            return toCrossCurrencyLedgerTransfers(postingInstruction, authorisationTimeoutSeconds);
        }

        return List.of(postingInstructionType.movesBetweenCustomerWallets()
                ? toWalletToWalletLedgerTransfer(postingInstruction, authorisationTimeoutSeconds)
                : toCustomerToInternalAccountLedgerTransfer(postingInstruction, authorisationTimeoutSeconds));
    }

    public static List<LedgerTransfer> toAuthorisationFollowUpLedgerTransfers(
            LedgerPostingInstruction postingInstruction,
            List<UUID> authorisationTransferIds) {

        List<LedgerTransfer> followUpTransfers = new ArrayList<>();

        for (int legIndex = 0; legIndex < authorisationTransferIds.size(); legIndex++) {
            followUpTransfers.add(newLedgerTransferBuilder(postingInstruction, legIndex)
                    .pendingTransferId(authorisationTransferIds.get(legIndex))
                    .build());
        }

        return followUpTransfers;
    }


    private static List<LedgerTransfer> toCrossCurrencyLedgerTransfers(
            LedgerPostingInstruction postingInstruction,
            int authorisationTimeoutSeconds) {

        Currency sellCurrency = postingInstruction.currency();
        Currency buyCurrency = postingInstruction.buyCurrency();

        LedgerTransfer sellLeg = newLedgerTransferBuilder(postingInstruction, SELL_LEG)
                .debitAccountId(LedgerAccountIds.deriveWalletAccountId(
                        postingInstruction.customerAccountId(),
                        sellCurrency))
                .creditAccountId(LedgerAccountIds.deriveInternalAccountId(
                        LedgerAccountType.FX_POSITION,
                        sellCurrency))
                .amount(postingInstruction.amount())
                .currency(sellCurrency)
                .timeoutSeconds(authorisationTimeoutSeconds)
                .build();

        LedgerTransfer buyLeg = newLedgerTransferBuilder(postingInstruction, BUY_LEG)
                .debitAccountId(LedgerAccountIds.deriveInternalAccountId(
                        LedgerAccountType.FX_POSITION, buyCurrency))
                .creditAccountId(LedgerAccountIds.deriveWalletAccountId(
                        postingInstruction.counterpartyCustomerAccountId(), buyCurrency))
                .amount(postingInstruction.buyAmount())
                .currency(buyCurrency)
                .timeoutSeconds(authorisationTimeoutSeconds)
                .build();

        return List.of(sellLeg, buyLeg);
    }

    private static LedgerTransfer toWalletToWalletLedgerTransfer(
            LedgerPostingInstruction postingInstruction,
            int authorisationTimeoutSeconds) {

        Currency currency = postingInstruction.currency();

        return newLedgerTransferBuilder(postingInstruction, SINGLE_LEG)
                .debitAccountId(LedgerAccountIds.deriveWalletAccountId(
                        postingInstruction.customerAccountId(), currency))
                .creditAccountId(LedgerAccountIds.deriveWalletAccountId(
                        postingInstruction.counterpartyCustomerAccountId(), currency))
                .amount(postingInstruction.amount())
                .currency(currency)
                .timeoutSeconds(authorisationTimeoutSeconds)
                .build();
    }

    private static LedgerTransfer toCustomerToInternalAccountLedgerTransfer(
            LedgerPostingInstruction postingInstruction,
            int authorisationTimeoutSeconds) {

        PostingInstructionType postingInstructionType = postingInstruction.postingInstructionType();
        Currency currency = postingInstruction.currency();

        UUID walletAccountId =
                LedgerAccountIds.deriveWalletAccountId(postingInstruction.customerAccountId(), currency);
        UUID internalAccountId =
                LedgerAccountIds.deriveInternalAccountId(resolveInternalAccountType(postingInstruction), currency);

        boolean inbound = postingInstructionType.isInbound();

        return newLedgerTransferBuilder(postingInstruction, SINGLE_LEG)
                .debitAccountId(inbound ? internalAccountId : walletAccountId)
                .creditAccountId(inbound ? walletAccountId : internalAccountId)
                .amount(postingInstruction.amount())
                .currency(currency)
                .timeoutSeconds(postingInstructionType.isAuthorisation() ? authorisationTimeoutSeconds : NO_TIMEOUT)
                .build();
    }

    private static LedgerTransfer.LedgerTransferBuilder newLedgerTransferBuilder(
            LedgerPostingInstruction postingInstruction,
            int legIndex) {

        UUID clientTransactionId = postingInstruction.clientTransactionId();
        PostingInstructionType postingInstructionType = postingInstruction.postingInstructionType();

        return LedgerTransfer.builder()
                .id(LedgerTransferIds.deriveTransferId(clientTransactionId, postingInstructionType, legIndex))
                .clientTransactionId(clientTransactionId)
                .postingInstructionType(postingInstructionType);
    }

    private static LedgerAccountType resolveInternalAccountType(LedgerPostingInstruction postingInstruction) {
        LedgerAccountType requestedInternalAccountType = postingInstruction.internalAccountType();

        if (requestedInternalAccountType == null) {
            return postingInstruction.postingInstructionType().isInbound()
                    ? LedgerAccountType.INBOUND_CLEARING
                    : LedgerAccountType.OUTBOUND_CLEARING;
        }

        if (!requestedInternalAccountType.isInternal()) {
            throw new IllegalArgumentException(requestedInternalAccountType + " is not an internal account type");
        }

        return requestedInternalAccountType;
    }
}
