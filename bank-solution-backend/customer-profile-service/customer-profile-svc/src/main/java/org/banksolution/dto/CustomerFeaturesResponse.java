package org.banksolution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Behavioural transaction features used for customer risk scoring")
public class CustomerFeaturesResponse {

    @Schema(description = "Customer the features describe", example = "8c6d2a91-1f3e-4b7a-a5c4-2e9d7f0b1c36")
    private String customerId;

    @Schema(description = "Account the features describe", example = "b1e4f7a2-5c8d-4a3b-9e6f-0d2c7a9b4e15")
    private String accountId;

    @Schema(description = "Number of transactions observed", example = "42")
    private int transactionCount;

    @Schema(description = "Sum of all transaction amounts", example = "12500.00")
    private double totalAmount;

    @Schema(description = "Mean transaction amount", example = "297.62")
    private double avgAmount;

    @Schema(description = "Median transaction amount", example = "180.00")
    private double medianAmount;

    @Schema(description = "Largest transaction amount", example = "4200.00")
    private double maxAmount;

    @Schema(description = "Smallest transaction amount", example = "5.00")
    private double minAmount;

    @Schema(description = "Standard deviation of transaction amounts", example = "512.40")
    private double stdAmount;

    @Schema(description = "Days with at least one transaction", example = "18")
    private int activeDays;

    @Schema(description = "Average transactions per active day", example = "2.33")
    private double transactionsPerDay;

    // Pattern features
    @Schema(description = "Share of cross-border transactions", example = "0.12")
    private double crossBorderRatio;

    @Schema(description = "Share of cash transactions", example = "0.05")
    private double cashTransactionRatio;

    @Schema(description = "Share of transactions above 10,000", example = "0.02")
    private double largeTransactionRatio;

    @Schema(description = "Share of transactions between 22:00 and 06:00", example = "0.08")
    private double nightTransactionRatio;

    @Schema(description = "Share of weekend transactions", example = "0.25")
    private double weekendTransactionRatio;

    // Diversity metrics
    @Schema(description = "Distinct receivers paid", example = "9")
    private int uniqueReceivers;

    @Schema(description = "Distinct receiver countries", example = "2")
    private int uniqueReceiverCountries;

    @Schema(description = "Spread of payments across receivers", example = "0.61")
    private double receiverDiversity;

    @Schema(description = "Distinct currencies transacted", example = "1")
    private int uniqueCurrencies;

    @Schema(description = "Stability of transaction amounts", example = "0.74")
    private double amountConsistency;

    // Metadata
    @Schema(description = "When the profile was last recalculated", example = "2026-09-27T10:15:30Z")
    private Instant lastUpdatedAt;
}