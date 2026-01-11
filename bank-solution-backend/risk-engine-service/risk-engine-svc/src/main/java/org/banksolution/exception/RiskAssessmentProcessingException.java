package org.banksolution.exception;

import lombok.Getter;

@Getter
public class RiskAssessmentProcessingException extends RuntimeException {

    private final String paymentId;

    public RiskAssessmentProcessingException(String paymentId, String message, Throwable cause) {
        super(message, cause);
        this.paymentId = paymentId;
    }

}
