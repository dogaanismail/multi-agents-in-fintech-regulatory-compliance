package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.LedgerPostingProperties;
import org.banksolution.domain.LedgerAccountIds;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.Currency;
import org.banksolution.enums.LedgerAccountType;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.banksolution.repository.TigerBeetleTransferRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static org.banksolution.enums.PostingInstructionType.INBOUND_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingService {

    private static final int NO_TIMEOUT = 0;

    private final TigerBeetleTransferRepository tigerBeetleTransferRepository;
    private final LedgerPostingProperties ledgerPostingProperties;

    public LedgerTransfer applyPostingInstruction(LedgerPostingInstruction postingInstruction) {
        log.info("Applying {} for client transaction {}",
                postingInstruction.postingInstructionType(), postingInstruction.clientTransactionId());

        LedgerTransfer ledgerTransfer = switch (postingInstruction.postingInstructionType().getTransferType()) {
            case PENDING, SINGLE_PHASE -> buildCustomerToInternalAccountTransfer(postingInstruction);
            case POST_PENDING, VOID_PENDING -> buildAuthorisationFollowUpTransfer(postingInstruction);
        };

        return tigerBeetleTransferRepository.persistLedgerTransfer(ledgerTransfer);
    }

    public List<LedgerTransfer> getPostingsByClientTransactionId(UUID clientTransactionId) {
        return tigerBeetleTransferRepository.findLedgerTransfersByClientTransactionId(clientTransactionId);
    }

    private LedgerTransfer buildCustomerToInternalAccountTransfer(LedgerPostingInstruction postingInstruction) {
        PostingInstructionType postingInstructionType = postingInstruction.postingInstructionType();
        Currency currency = postingInstruction.currency();

        UUID walletAccountId =
                LedgerAccountIds.deriveWalletAccountId(postingInstruction.customerAccountId(), currency);
        UUID internalAccountId =
                LedgerAccountIds.deriveInternalAccountId(resolveInternalAccountType(postingInstruction), currency);

        boolean inbound = postingInstructionType.isInbound();

        return newLedgerTransferBuilder(postingInstruction)
                .debitAccountId(inbound ? internalAccountId : walletAccountId)
                .creditAccountId(inbound ? walletAccountId : internalAccountId)
                .amount(postingInstruction.amount())
                .currency(currency)
                .timeoutSeconds(authorisationTimeoutSecondsFor(postingInstructionType))
                .build();
    }

    private LedgerTransfer buildAuthorisationFollowUpTransfer(LedgerPostingInstruction postingInstruction) {
        UUID clientTransactionId = postingInstruction.clientTransactionId();

        return newLedgerTransferBuilder(postingInstruction)
                .pendingTransferId(findAuthorisationTransferId(clientTransactionId))
                .build();
    }

    /**
     * Authorisation ids are derived, so both directions can be probed in a single lookup
     * instead of searching the ledger for them.
     */
    private UUID findAuthorisationTransferId(UUID clientTransactionId) {
        List<UUID> candidateTransferIds = List.of(
                LedgerTransferIds.deriveTransferId(clientTransactionId, INBOUND_AUTHORISATION),
                LedgerTransferIds.deriveTransferId(clientTransactionId, OUTBOUND_AUTHORISATION));

        return tigerBeetleTransferRepository.findLedgerTransfersByIds(candidateTransferIds).stream()
                .filter(ledgerTransfer -> ledgerTransfer.postingInstructionType().isAuthorisation())
                .map(LedgerTransfer::id)
                .findFirst()
                .orElseThrow(() -> new PendingAuthorisationNotFoundException(clientTransactionId));
    }

    private LedgerTransfer.LedgerTransferBuilder newLedgerTransferBuilder(
            LedgerPostingInstruction postingInstruction) {
        UUID clientTransactionId = postingInstruction.clientTransactionId();
        PostingInstructionType postingInstructionType = postingInstruction.postingInstructionType();

        return LedgerTransfer.builder()
                .id(LedgerTransferIds.deriveTransferId(clientTransactionId, postingInstructionType))
                .clientTransactionId(clientTransactionId)
                .postingInstructionType(postingInstructionType);
    }

    private int authorisationTimeoutSecondsFor(PostingInstructionType postingInstructionType) {
        return postingInstructionType.isAuthorisation()
                ? ledgerPostingProperties.authorisationTimeoutSeconds()
                : NO_TIMEOUT;
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
