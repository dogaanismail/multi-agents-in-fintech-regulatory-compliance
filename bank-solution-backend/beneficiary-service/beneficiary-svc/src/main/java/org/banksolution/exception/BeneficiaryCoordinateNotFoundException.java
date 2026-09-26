package org.banksolution.exception;

import java.util.UUID;

public class BeneficiaryCoordinateNotFoundException extends RuntimeException {

    public BeneficiaryCoordinateNotFoundException(UUID beneficiaryId, UUID beneficiaryCoordinateId) {
        super("Coordinate " + beneficiaryCoordinateId + " not found on beneficiary " + beneficiaryId);
    }
}
