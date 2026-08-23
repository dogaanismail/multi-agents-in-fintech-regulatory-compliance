package org.banksolution.service;

import com.aml.ledger.LedgerPostingCompletedEvent;
import com.aml.ledger.LedgerPostingRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.domain.LedgerTransfer;
import org.banksolution.exception.InsufficientLedgerFundsException;
import org.banksolution.exception.LedgerPostingException;
import org.banksolution.exception.PendingAuthorisationNotFoundException;
import org.banksolution.infrastructure.messaging.kafka.producer.LedgerPostingCompletedEventProducer;
import org.banksolution.mapper.LedgerPostingEventMapper;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerPostingRequestService {

    private final LedgerPostingService ledgerPostingService;
    private final LedgerPostingCompletedEventProducer ledgerPostingCompletedEventProducer;

    public void processLedgerPostingRequest(LedgerPostingRequestedEvent event) {
        LedgerPostingInstruction postingInstruction =
                LedgerPostingEventMapper.toLedgerPostingInstruction(event);

        try {
            List<LedgerTransfer> ledgerTransfers = ledgerPostingService.applyPostingInstruction(postingInstruction);
            publishSuccess(ledgerTransfers.getFirst());
        } catch (InsufficientLedgerFundsException | PendingAuthorisationNotFoundException | LedgerPostingException e) {
            publishRejection(event, e.getMessage());
        }
    }

    private void publishSuccess(LedgerTransfer ledgerTransfer) {
        ledgerPostingCompletedEventProducer.publish(
                LedgerPostingEventMapper.toSuccessfulLedgerPostingCompletedEvent(ledgerTransfer));
    }

    private void publishRejection(LedgerPostingRequestedEvent event, String failureReason) {
        log.warn("Ledger rejected {} for client transaction {}: {}",
                event.getPostingInstructionType(), event.getClientTransactionId(), failureReason);

        LedgerPostingCompletedEvent rejection =
                LedgerPostingEventMapper.toFailedLedgerPostingCompletedEvent(event, failureReason);

        ledgerPostingCompletedEventProducer.publish(rejection);
    }
}
