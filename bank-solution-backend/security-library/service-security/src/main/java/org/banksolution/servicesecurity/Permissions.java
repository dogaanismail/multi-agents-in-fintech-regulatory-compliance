package org.banksolution.servicesecurity;

public final class Permissions {

    public static final String PAYMENT_READ = "payment.read";
    public static final String CUSTOMER_READ = "customer.read";
    public static final String ACCOUNT_READ = "account.read";
    public static final String LEDGER_READ = "ledger.read";
    public static final String RISK_READ = "risk.read";
    public static final String MARL_READ = "marl.read";
    public static final String CONFIGURATION_READ = "configuration.read";
    public static final String CUSTOMER_CREATE = "customer.create";
    public static final String CUSTOMER_UPDATE = "customer.update";
    public static final String CUSTOMER_DELETE = "customer.delete";
    public static final String ACCOUNT_OPEN = "account.open";
    public static final String PAYMENT_CREATE = "payment.create";
    public static final String PAYMENT_REVIEW = "payment.review";
    public static final String PAYMENT_OVERRIDE = "payment.override";
    public static final String CONFIGURATION_WRITE = "configuration.write";
    public static final String MARL_TRAIN = "marl.train";
    public static final String LEDGER_POST = "ledger.post";
    public static final String BENEFICIARY_WRITE = "beneficiary.write";
    public static final String PAYMENT_ENGINE_COMMAND = "payment-engine.command";
    public static final String IAM_MANAGE = "iam.manage";

    private Permissions() {
    }
}
