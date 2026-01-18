package org.banksolution.mapper;

import lombok.experimental.UtilityClass;
import org.banksolution.dto.CustomerFeaturesResponse;
import org.banksolution.entity.CustomerProfileEntity;

import java.math.BigDecimal;
import java.util.UUID;

@UtilityClass
public class CustomerProfileMapper {

    public CustomerFeaturesResponse toResponse(CustomerProfileEntity profile) {
        return CustomerFeaturesResponse.builder()
                .customerId(profile.getCustomerId() != null ? profile.getCustomerId().toString() : null)
                .accountId(profile.getAccountId() != null ? profile.getAccountId().toString() : null)
                .transactionCount(profile.getTransactionCount())
                .totalAmount(profile.getTotalAmount().doubleValue())
                .avgAmount(profile.getAvgAmount().doubleValue())
                .medianAmount(profile.getMedianAmount().doubleValue())
                .maxAmount(profile.getMaxAmount().doubleValue())
                .minAmount(profile.getMinAmount().doubleValue())
                .stdAmount(profile.getStdAmount().doubleValue())
                .activeDays(profile.getActiveDays())
                .transactionsPerDay(profile.getTransactionsPerDay().doubleValue())
                .crossBorderRatio(profile.getCrossBorderRatio().doubleValue())
                .cashTransactionRatio(profile.getCashTransactionRatio().doubleValue())
                .amountConsistency(profile.getAmountConsistency().doubleValue())
                .largeTransactionRatio(profile.getLargeTransactionRatio().doubleValue())
                .uniqueReceivers(profile.getUniqueReceivers())
                .uniqueReceiverCountries(profile.getUniqueReceiverCountries())
                .receiverDiversity(profile.getReceiverDiversity().doubleValue())
                .nightTransactionRatio(profile.getNightTransactionRatio().doubleValue())
                .weekendTransactionRatio(profile.getWeekendTransactionRatio().doubleValue())
                .uniqueCurrencies(profile.getUniqueCurrencies())
                .lastUpdatedAt(profile.getLastUpdatedAt())
                .build();
    }

    public CustomerProfileEntity createDefaultProfile(UUID customerId, UUID accountId) {
        return CustomerProfileEntity.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .accountId(accountId)
                .transactionCount(0)
                .totalAmount(BigDecimal.ZERO)
                .avgAmount(BigDecimal.ZERO)
                .medianAmount(BigDecimal.ZERO)
                .maxAmount(BigDecimal.ZERO)
                .minAmount(BigDecimal.ZERO)
                .stdAmount(BigDecimal.ZERO)
                .activeDays(0)
                .transactionsPerDay(BigDecimal.ZERO)
                .crossBorderCount(0)
                .crossBorderRatio(BigDecimal.ZERO)
                .cashTransactionCount(0)
                .cashTransactionRatio(BigDecimal.ZERO)
                .largeTransactionCount(0)
                .largeTransactionRatio(BigDecimal.ZERO)
                .nightTransactionCount(0)
                .nightTransactionRatio(BigDecimal.ZERO)
                .weekendTransactionCount(0)
                .weekendTransactionRatio(BigDecimal.ZERO)
                .uniqueReceivers(0)
                .uniqueReceiverCountries(0)
                .receiverDiversity(BigDecimal.ZERO)
                .uniqueCurrencies(0)
                .amountConsistency(BigDecimal.ZERO)
                .build();
    }
}