package org.banksolution.mapper;

import org.banksolution.domain.LedgerPostingInstruction;
import org.banksolution.model.request.CreateLedgerPostingInstructionRequest;
import org.banksolution.model.request.CustomerAccountMovementRequest;
import org.banksolution.model.request.InternalTransferMovementRequest;

import java.util.UUID;

public final class LedgerPostingInstructionMapper {

    private LedgerPostingInstructionMapper() {
    }

    public static LedgerPostingInstruction toLedgerPostingInstruction(
            CreateLedgerPostingInstructionRequest request) {

        UUID clientTransactionId = request.getClientTransactionId();

        if (request.getInboundAuthorisation() != null) {
            CustomerAccountMovementRequest movement = request.getInboundAuthorisation();
            return LedgerPostingInstruction.inboundAuthorisation(clientTransactionId,
                    movement.getAmount(),
                    movement.getCurrency(),
                    movement.getCustomerAccountId(),
                    movement.getInternalAccountType()
            );
        }

        if (request.getOutboundAuthorisation() != null) {
            CustomerAccountMovementRequest movement = request.getOutboundAuthorisation();
            return LedgerPostingInstruction.outboundAuthorisation(clientTransactionId,
                    movement.getAmount(),
                    movement.getCurrency(),
                    movement.getCustomerAccountId(),
                    movement.getInternalAccountType()
            );
        }

        if (request.getInboundHardSettlement() != null) {
            CustomerAccountMovementRequest movement = request.getInboundHardSettlement();
            return LedgerPostingInstruction.inboundHardSettlement(clientTransactionId,
                    movement.getAmount(),
                    movement.getCurrency(),
                    movement.getCustomerAccountId(),
                    movement.getInternalAccountType()
            );
        }

        if (request.getOutboundHardSettlement() != null) {
            CustomerAccountMovementRequest movement = request.getOutboundHardSettlement();
            return LedgerPostingInstruction.outboundHardSettlement(clientTransactionId,
                    movement.getAmount(),
                    movement.getCurrency(),
                    movement.getCustomerAccountId(),
                    movement.getInternalAccountType()
            );
        }

        if (request.getInternalTransferAuthorisation() != null) {
            InternalTransferMovementRequest movement = request.getInternalTransferAuthorisation();
            return LedgerPostingInstruction.internalTransferAuthorisation(clientTransactionId,
                    movement.getAmount(),
                    movement.getCurrency(),
                    movement.getSourceCustomerAccountId(),
                    movement.getDestinationCustomerAccountId()
            );
        }

        if (request.getSettlement() != null) {
            return LedgerPostingInstruction.settlement(clientTransactionId);
        }

        if (request.getRelease() != null) {
            return LedgerPostingInstruction.release(clientTransactionId);
        }

        throw new IllegalArgumentException("Exactly one posting instruction type must be provided");
    }
}
