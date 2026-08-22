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

    LEDGER_AUTHORISATION_INITIATED("Ledger authorisation was initiated"),
    LEDGER_AUTHORISED("Funds were authorised on the ledger"),
    LEDGER_AUTHORISATION_DECLINED("Ledger declined the authorisation"),
    LEDGER_SETTLEMENT_INITIATED("Ledger settlement was initiated"),
    LEDGER_SETTLED("Funds were settled on the ledger"),
    LEDGER_SETTLEMENT_FAILED("Ledger settlement failed"),
    LEDGER_RELEASE_INITIATED("Ledger release was initiated"),
    LEDGER_RELEASED("Authorised funds were released on the ledger"),

    DECISION_OVERRIDE_APPROVED("Compliance officer overrode the decision and approved the payment"),
    DECISION_OVERRIDE_REJECTED("Compliance officer overrode the decision and rejected the payment");

    private final String description;

    PaymentEventTrigger(String description) {
        this.description = description;
    }

}
