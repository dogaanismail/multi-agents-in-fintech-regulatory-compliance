package org.banksolution.fixtures;

import com.aml.fraud.TransactionFeatures;

public final class TransactionFeaturesFixtures {

    private TransactionFeaturesFixtures() {
    }

    public static TransactionFeatures createTransactionFeatures(
            String paymentId,
            String senderBankLocation,
            String receiverBankLocation) {

        return TransactionFeatures.newBuilder()
                .setPaymentId(paymentId)
                .setTime("14:30:00")
                .setDate("2026-08-25")
                .setSenderAccount("GB0001")
                .setReceiverAccount("DE0002")
                .setAmount(1500.50)
                .setPaymentCurrency(RiskCheckRequestFixtures.FROM_CURRENCY)
                .setReceivedCurrency(RiskCheckRequestFixtures.TO_CURRENCY)
                .setSenderBankLocation(senderBankLocation)
                .setReceiverBankLocation(receiverBankLocation)
                .setPaymentType("ACH")
                .build();
    }
}
