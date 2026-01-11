package org.banksolution.exception;

public class FraudAnalysisCompletedEventException extends RuntimeException {

    public FraudAnalysisCompletedEventException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }
}
