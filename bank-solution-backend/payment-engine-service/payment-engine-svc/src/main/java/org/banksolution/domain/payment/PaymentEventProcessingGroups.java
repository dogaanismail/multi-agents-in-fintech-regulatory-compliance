package org.banksolution.domain.payment;

import java.util.List;


public final class PaymentEventProcessingGroups {

    public static final String PAYMENT_SNAPSHOT_PUBLISHER = "payment-snapshot-publisher";
    public static final String COMPLIANCE_FEEDBACK_PUBLISHER = "compliance-feedback-publisher";
    public static final String PAYMENT_RISK_SAGA = "payment-risk-saga";
    public static final String LEDGER_POSTING_SAGA = "ledger-posting-saga";

    private PaymentEventProcessingGroups() {
    }

    public static List<String> deadLetteringGroups() {
        return List.of(
                PAYMENT_SNAPSHOT_PUBLISHER,
                COMPLIANCE_FEEDBACK_PUBLISHER
        );
    }

    public static List<String> sagaGroups() {
        return List.of(
                PAYMENT_RISK_SAGA,
                LEDGER_POSTING_SAGA
        );
    }

    public static List<String> allGroups() {
        return List.of(
                PAYMENT_SNAPSHOT_PUBLISHER,
                COMPLIANCE_FEEDBACK_PUBLISHER,
                PAYMENT_RISK_SAGA,
                LEDGER_POSTING_SAGA
        );
    }
}
