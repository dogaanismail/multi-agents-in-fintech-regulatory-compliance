package org.banksolution.exception;

import lombok.Getter;

@Getter
public class RiskAssessmentRequestedEventException extends RuntimeException {

    public RiskAssessmentRequestedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
