package org.banksolution.mapper;

import com.aml.fraud.CustomerFeatures;
import lombok.experimental.UtilityClass;
import org.banksolution.integration.customerprofile.dto.CustomerFeaturesResponse;

@UtilityClass
public class CustomerFeaturesMapper {

    public CustomerFeatures toAvroCustomerFeatures(CustomerFeaturesResponse response) {
        return CustomerFeatures.newBuilder()
                .setCustomerId(response.getCustomerId())
                .setAccountId(response.getAccountId())
                .setTransactionCount(response.getTransactionCount())
                .setTotalAmount(response.getTotalAmount())
                .setAvgAmount(response.getAvgAmount())
                .setMedianAmount(response.getMedianAmount())
                .setMaxAmount(response.getMaxAmount())
                .setMinAmount(response.getMinAmount())
                .setStdAmount(response.getStdAmount())
                .setActiveDays(response.getActiveDays())
                .setTransactionsPerDay(response.getTransactionsPerDay())
                .setCrossBorderRatio(response.getCrossBorderRatio())
                .setCashTransactionRatio(response.getCashTransactionRatio())
                .setLargeTransactionRatio(response.getLargeTransactionRatio())
                .setNightTransactionRatio(response.getNightTransactionRatio())
                .setWeekendTransactionRatio(response.getWeekendTransactionRatio())
                .setUniqueReceivers(response.getUniqueReceivers())
                .setUniqueReceiverCountries(response.getUniqueReceiverCountries())
                .setReceiverDiversity(response.getReceiverDiversity())
                .setUniqueCurrencies(response.getUniqueCurrencies())
                .setAmountConsistency(response.getAmountConsistency())
                .build();
    }

    public CustomerFeatures getDefaultCustomerFeatures(String customerId, String accountId) {
        return CustomerFeatures.newBuilder()
                .setCustomerId(customerId)
                .setAccountId(accountId)
                .setTransactionCount(0)
                .setTotalAmount(0.0)
                .setAvgAmount(0.0)
                .setMedianAmount(0.0)
                .setMaxAmount(0.0)
                .setMinAmount(0.0)
                .setStdAmount(0.0)
                .setActiveDays(0)
                .setTransactionsPerDay(0.0)
                .setCrossBorderRatio(0.0)
                .setCashTransactionRatio(0.0)
                .setLargeTransactionRatio(0.0)
                .setNightTransactionRatio(0.0)
                .setWeekendTransactionRatio(0.0)
                .setUniqueReceivers(0)
                .setUniqueReceiverCountries(0)
                .setReceiverDiversity(0.0)
                .setUniqueCurrencies(0)
                .setAmountConsistency(0.0)
                .build();
    }
}