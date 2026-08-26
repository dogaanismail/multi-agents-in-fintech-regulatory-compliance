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
            CreateLedgerPostingInstructionRequest createLedgerPostingInstructionRequest) {

        UUID clientTransactionId = createLedgerPostingInstructionRequest.getClientTransactionId();

        if (createLedgerPostingInstructionRequest.getInboundAuthorisation() != null) {
            CustomerAccountMovementRequest customerAccountMovementRequest = createLedgerPostingInstructionRequest.getInboundAuthorisation();
            return LedgerPostingInstruction.inboundAuthorisation(clientTransactionId,
                    customerAccountMovementRequest.getAmount(),
                    customerAccountMovementRequest.getCurrency(),
                    customerAccountMovementRequest.getCustomerAccountId(),
                    customerAccountMovementRequest.getInternalAccountType()
            );
        }

        if (createLedgerPostingInstructionRequest.getOutboundAuthorisation() != null) {
            CustomerAccountMovementRequest customerAccountMovementRequest = createLedgerPostingInstructionRequest.getOutboundAuthorisation();
            return LedgerPostingInstruction.outboundAuthorisation(clientTransactionId,
                    customerAccountMovementRequest.getAmount(),
                    customerAccountMovementRequest.getCurrency(),
                    customerAccountMovementRequest.getCustomerAccountId(),
                    customerAccountMovementRequest.getInternalAccountType()
            );
        }

        if (createLedgerPostingInstructionRequest.getInboundHardSettlement() != null) {
            CustomerAccountMovementRequest customerAccountMovementRequest = createLedgerPostingInstructionRequest.getInboundHardSettlement();
            return LedgerPostingInstruction.inboundHardSettlement(clientTransactionId,
                    customerAccountMovementRequest.getAmount(),
                    customerAccountMovementRequest.getCurrency(),
                    customerAccountMovementRequest.getCustomerAccountId(),
                    customerAccountMovementRequest.getInternalAccountType()
            );
        }

        if (createLedgerPostingInstructionRequest.getOutboundHardSettlement() != null) {
            CustomerAccountMovementRequest customerAccountMovementRequest = createLedgerPostingInstructionRequest.getOutboundHardSettlement();
            return LedgerPostingInstruction.outboundHardSettlement(clientTransactionId,
                    customerAccountMovementRequest.getAmount(),
                    customerAccountMovementRequest.getCurrency(),
                    customerAccountMovementRequest.getCustomerAccountId(),
                    customerAccountMovementRequest.getInternalAccountType()
            );
        }

        if (createLedgerPostingInstructionRequest.getInternalTransferAuthorisation() != null) {
            InternalTransferMovementRequest internalTransferMovementRequest = createLedgerPostingInstructionRequest.getInternalTransferAuthorisation();
            return LedgerPostingInstruction.internalTransferAuthorisation(clientTransactionId,
                    internalTransferMovementRequest.getAmount(),
                    internalTransferMovementRequest.getCurrency(),
                    internalTransferMovementRequest.getSourceCustomerAccountId(),
                    internalTransferMovementRequest.getDestinationCustomerAccountId()
            );
        }

        if (createLedgerPostingInstructionRequest.getSettlement() != null) {
            return LedgerPostingInstruction.settlement(clientTransactionId);
        }

        if (createLedgerPostingInstructionRequest.getRelease() != null) {
            return LedgerPostingInstruction.release(clientTransactionId);
        }

        throw new IllegalArgumentException("Exactly one posting instruction type must be provided");
    }
}
