package org.banksolution.exception;

import lombok.Getter;

@Getter
public class RiskAssessmentProcessingException extends RuntimeException {

    public RiskAssessmentProcessingException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
