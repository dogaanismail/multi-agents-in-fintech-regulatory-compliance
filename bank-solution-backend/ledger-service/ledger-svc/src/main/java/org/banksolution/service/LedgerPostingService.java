package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.config.LedgerPostingProperties;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.domain.LedgerTransferIds;
import org.banksolution.enums.PostingInstructionType;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.banksolution.mapper.LedgerPostingTransferMapper;
import org.banksolution.repository.TigerBeetleTransferRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.banksolution.domain.LedgerTransferIds.MAX_LEGS_PER_INSTRUCTION;
import static org.banksolution.enums.PostingInstructionType.CROSS_CURRENCY_TRANSFER_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.INBOUND_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.INTERNAL_TRANSFER_AUTHORISATION;
import static org.banksolution.enums.PostingInstructionType.OUTBOUND_AUTHORISATION;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingService {

    private static final List<PostingInstructionType> AUTHORISATION_TYPES = List.of(
            INBOUND_AUTHORISATION,
            OUTBOUND_AUTHORISATION,
            INTERNAL_TRANSFER_AUTHORISATION,
            CROSS_CURRENCY_TRANSFER_AUTHORISATION);

    private final TigerBeetleTransferRepository tigerBeetleTransferRepository;
    private final LedgerPostingProperties ledgerPostingProperties;

    public List<LedgerTransfer> applyPostingInstruction(LedgerPostingInstruction postingInstruction) {
        log.info("Applying {} for client transaction {}",
                postingInstruction.postingInstructionType(), postingInstruction.clientTransactionId());

        List<LedgerTransfer> ledgerTransfers = toLedgerTransfers(postingInstruction);

        return tigerBeetleTransferRepository.persistLinkedLedgerTransfers(ledgerTransfers);
    }

    public List<LedgerTransfer> getPostingsByClientTransactionId(UUID clientTransactionId) {
        return tigerBeetleTransferRepository.findLedgerTransfersByClientTransactionId(clientTransactionId);
    }

    private List<LedgerTransfer> toLedgerTransfers(LedgerPostingInstruction postingInstruction) {
        return switch (postingInstruction.postingInstructionType().getTransferType()) {
            case PENDING, SINGLE_PHASE -> LedgerPostingTransferMapper.toMovementLedgerTransfers(
                    postingInstruction,
                    ledgerPostingProperties.authorisationTimeoutSeconds());
            case POST_PENDING, VOID_PENDING -> LedgerPostingTransferMapper.toAuthorisationFollowUpLedgerTransfers(
                    postingInstruction,
                    findAuthorisationTransferIds(postingInstruction.clientTransactionId()));
        };
    }

    private List<UUID> findAuthorisationTransferIds(UUID clientTransactionId) {
        List<UUID> candidateTransferIds = AUTHORISATION_TYPES.stream()
                .flatMap(postingInstructionType -> IntStream.range(0, MAX_LEGS_PER_INSTRUCTION)
                        .mapToObj(legIndex -> LedgerTransferIds.deriveTransferId(
                                clientTransactionId,
                                postingInstructionType,
                                legIndex)))
                .toList();

        List<UUID> authorisationTransferIds =
                tigerBeetleTransferRepository.findLedgerTransfersByIds(candidateTransferIds).stream()
                        .filter(ledgerTransfer -> ledgerTransfer.postingInstructionType().isAuthorisation())
                        .map(LedgerTransfer::id)
                        .toList();

        if (authorisationTransferIds.isEmpty()) {
            throw new PendingAuthorisationNotFoundException(clientTransactionId);
        }

        return authorisationTransferIds;
    }
}
