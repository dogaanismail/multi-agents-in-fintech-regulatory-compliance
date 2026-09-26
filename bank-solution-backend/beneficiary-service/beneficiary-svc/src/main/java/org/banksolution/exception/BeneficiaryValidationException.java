package org.banksolution.exception;

import java.util.List;

import lombok.Getter;

@Getter
public class BeneficiaryValidationException extends RuntimeException {

    private final transient List<String> violations;

    public BeneficiaryValidationException(List<String> violations) {
        super("Beneficiary is invalid: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }
}
