package org.banksolution.enums;

import lombok.Getter;

@Getter
public enum PaymentEventTrigger {

    PAYMENT_INITIATED("Payment was initiated"),
    PAYMENT_COMPLETED("Payment was successfully completed"),
    PAYMENT_BLOCKED("Payment was blocked due to risk assessment"),

    RISK_ASSESSMENT_INITIATED("Risk assessment was initiated"),
    RISK_ASSESSMENT_COMPLETED("Risk assessment was completed"),

    FRAUD_CHECK_APPROVED("Fraud check passed"),

    MANUAL_REVIEW_REQUESTED("Payment requires manual review"),
    MANUAL_REVIEW_APPROVED("Manual review approved the payment"),
    MANUAL_REVIEW_REJECTED("Manual review rejected the payment"),

    ACCOUNT_CHARGE_INITIATED("Account charge was initiated"),
    ACCOUNT_CHARGED("Account was successfully charged"),
    ACCOUNT_CHARGE_FAILED("Account charge failed"),

    DECISION_OVERRIDE_APPROVED("Compliance officer overrode the decision and approved the payment"),
    DECISION_OVERRIDE_REJECTED("Compliance officer overrode the decision and rejected the payment");

    private final String description;

    PaymentEventTrigger(String description) {
        this.description = description;
    }

}
