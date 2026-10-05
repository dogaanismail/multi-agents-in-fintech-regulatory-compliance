package org.banksolution.security;

public enum Permission {

    PAYMENT_READ("payment.read"),
    CUSTOMER_READ("customer.read"),
    ACCOUNT_READ("account.read"),
    LEDGER_READ("ledger.read"),
    RISK_READ("risk.read"),
    MARL_READ("marl.read"),
    CONFIGURATION_READ("configuration.read"),
    CUSTOMER_CREATE("customer.create"),
    CUSTOMER_UPDATE("customer.update"),
    CUSTOMER_DELETE("customer.delete"),
    ACCOUNT_OPEN("account.open"),
    PAYMENT_CREATE("payment.create"),
    PAYMENT_REVIEW("payment.review"),
    PAYMENT_OVERRIDE("payment.override"),
    CONFIGURATION_WRITE("configuration.write"),
    MARL_TRAIN("marl.train"),
    LEDGER_POST("ledger.post"),
    BENEFICIARY_WRITE("beneficiary.write"),
    PAYMENT_ENGINE_COMMAND("payment-engine.command"),
    IAM_MANAGE("iam.manage");

    private final String authority;

    Permission(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
