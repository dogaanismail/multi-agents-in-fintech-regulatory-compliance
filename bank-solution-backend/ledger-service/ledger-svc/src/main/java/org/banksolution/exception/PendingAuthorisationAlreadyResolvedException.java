package org.banksolution.exception;

import org.banksolution.enums.PostingInstructionType;

import java.util.UUID;

public class PendingAuthorisationAlreadyResolvedException extends RuntimeException {
    public PendingAuthorisationAlreadyResolvedException(
            UUID clientTransactionId,
            PostingInstructionType postingInstructionType,
            String resolution) {

        super("Authorisation for client transaction " + clientTransactionId + " was already " + resolution
                + "; cannot apply " + postingInstructionType);
    }
}
