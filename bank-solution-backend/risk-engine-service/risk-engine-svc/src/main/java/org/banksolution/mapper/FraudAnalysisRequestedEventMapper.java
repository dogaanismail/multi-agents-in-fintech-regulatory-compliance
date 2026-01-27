package org.banksolution.mapper;

import com.aml.fraud.CustomerFeatures;
import com.aml.fraud.FraudAnalysisRequestedEvent;
import com.aml.fraud.NetworkFeatures;
import com.aml.fraud.TransactionFeatures;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FraudAnalysisRequestedEventMapper {

    public FraudAnalysisRequestedEvent toAvroRequest(
            String riskCheckRequestId,
            Long timestamp,
            TransactionFeatures transactionFeatures,
            CustomerFeatures customerFeatures,
            NetworkFeatures networkFeatures) {

        return FraudAnalysisRequestedEvent.newBuilder()
                .setPaymentId(transactionFeatures.getPaymentId())
                .setRiskCheckRequestId(riskCheckRequestId)
                .setTimestamp(timestamp)
                .setIsCrossBorderPayment(isCrossBorder(transactionFeatures.getSenderBankLocation(), transactionFeatures.getSenderBankLocation()))
                .setTransactionFeatures(transactionFeatures)
                .setCustomerFeatures(customerFeatures)
                .setNetworkFeatures(networkFeatures)
                .build();
    }

    public boolean isCrossBorder(String senderLocation, String receiverLocation) {
        if (senderLocation == null || receiverLocation == null) {
            return false;
        }

        return !senderLocation.equalsIgnoreCase(receiverLocation);
    }
}
