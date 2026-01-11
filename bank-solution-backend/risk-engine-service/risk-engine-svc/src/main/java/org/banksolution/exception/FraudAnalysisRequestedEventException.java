package org.banksolution.exception;

public class FraudAnalysisRequestedEventException extends RuntimeException {

    public FraudAnalysisRequestedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }

}
