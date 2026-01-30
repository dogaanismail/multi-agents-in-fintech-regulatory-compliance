package org.banksolution.exception;

public class RiskAssessmentCompletedEventException extends RuntimeException {

    public RiskAssessmentCompletedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
