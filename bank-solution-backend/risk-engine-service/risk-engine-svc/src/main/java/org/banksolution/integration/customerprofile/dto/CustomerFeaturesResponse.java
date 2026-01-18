package org.banksolution.integration.customerprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFeaturesResponse {

    private String customerId;
    private String accountId;

    // Basic statistics
    private int transactionCount;
    private double totalAmount;
    private double avgAmount;
    private double medianAmount;
    private double maxAmount;
    private double minAmount;
    private double stdAmount;

    // Temporal features
    private int activeDays;
    private double transactionsPerDay;

    // Pattern features
    private double crossBorderRatio;
    private double cashTransactionRatio;
    private double largeTransactionRatio;
    private double nightTransactionRatio;
    private double weekendTransactionRatio;

    // Diversity metrics
    private int uniqueReceivers;
    private int uniqueReceiverCountries;
    private double receiverDiversity;
    private int uniqueCurrencies;
    private double amountConsistency;

    // Metadata
    private Instant lastUpdatedAt;
}