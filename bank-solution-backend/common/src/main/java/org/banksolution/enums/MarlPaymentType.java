package org.banksolution.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarlPaymentType {

    ACH("ACH"),
    CROSS_BORDER("Cross-border"),
    CASH_DEPOSIT("Cash Deposit"),
    CASH_WITHDRAWAL("Cash Withdrawal"),
    CHEQUE("Cheque"),
    CREDIT_CARD("Credit card"),
    DEBIT_CARD("Debit card");

    private final String value;

    @Override
    public String toString() {
        return value;
    }
}
