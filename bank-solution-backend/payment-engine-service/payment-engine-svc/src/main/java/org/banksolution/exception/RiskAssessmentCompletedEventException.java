package org.banksolution.exception;

import lombok.Getter;

@Getter
public class RiskAssessmentCompletedEventException extends RuntimeException {

    public RiskAssessmentCompletedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
