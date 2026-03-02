package org.banksolution.service;

import lombok.extern.slf4j.Slf4j;
import org.banksolution.entity.CustomerProfileEntity;
import org.banksolution.entity.TransactionSnapshotEntity;
import org.banksolution.mapper.CustomerProfileMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.banksolution.util.StatisticsUtil.*;

@Service
@Slf4j
public class FeatureCalculationService {

    public void calculateAndUpdateFeatures(
            CustomerProfileEntity profile,
            List<TransactionSnapshotEntity> transactions) {

        if (transactions.isEmpty()) {
            applyDefaultFeatures(profile);
            return;
        }

        calculateAmountFeatures(profile, transactions);
        calculateTemporalFeatures(profile, transactions);
        calculateClassificationFeatures(profile, transactions);
        calculateDiversityFeatures(profile, transactions);
    }

    private void applyDefaultFeatures(CustomerProfileEntity profile) {
        CustomerProfileEntity defaults = CustomerProfileMapper.createDefaultProfile(
                profile.getCustomerId(),
                profile.getAccountId()
        );

        profile.setTransactionCount(defaults.getTransactionCount());
        profile.setTotalAmount(defaults.getTotalAmount());
        profile.setAvgAmount(defaults.getAvgAmount());
        profile.setMedianAmount(defaults.getMedianAmount());
        profile.setMaxAmount(defaults.getMaxAmount());
        profile.setMinAmount(defaults.getMinAmount());
        profile.setStdAmount(defaults.getStdAmount());
        profile.setActiveDays(defaults.getActiveDays());
        profile.setTransactionsPerDay(defaults.getTransactionsPerDay());
        profile.setCrossBorderCount(defaults.getCrossBorderCount());
        profile.setCrossBorderRatio(defaults.getCrossBorderRatio());
        profile.setCashTransactionCount(defaults.getCashTransactionCount());
        profile.setCashTransactionRatio(defaults.getCashTransactionRatio());
        profile.setLargeTransactionCount(defaults.getLargeTransactionCount());
        profile.setLargeTransactionRatio(defaults.getLargeTransactionRatio());
        profile.setNightTransactionCount(defaults.getNightTransactionCount());
        profile.setNightTransactionRatio(defaults.getNightTransactionRatio());
        profile.setWeekendTransactionCount(defaults.getWeekendTransactionCount());
        profile.setWeekendTransactionRatio(defaults.getWeekendTransactionRatio());
        profile.setUniqueReceivers(defaults.getUniqueReceivers());
        profile.setUniqueReceiverCountries(defaults.getUniqueReceiverCountries());
        profile.setReceiverDiversity(defaults.getReceiverDiversity());
        profile.setUniqueCurrencies(defaults.getUniqueCurrencies());
        profile.setAmountConsistency(defaults.getAmountConsistency());
    }

    private void calculateAmountFeatures(
            CustomerProfileEntity profile,
            List<TransactionSnapshotEntity> transactions) {

        profile.setTransactionCount(transactions.size());

        List<BigDecimal> amounts = transactions.stream()
                .map(TransactionSnapshotEntity::getAmount)
                .toList();

        profile.setTotalAmount(sum(amounts));
        profile.setAvgAmount(mean(amounts));
        profile.setMaxAmount(max(amounts));
        profile.setMinAmount(min(amounts));
        profile.setMedianAmount(median(amounts));
        profile.setStdAmount(standardDeviation(amounts, profile.getAvgAmount()));
        profile.setAmountConsistency(calculateConsistency(profile.getStdAmount(), profile.getAvgAmount()));
    }

    private void calculateTemporalFeatures(
            CustomerProfileEntity profile,
            List<TransactionSnapshotEntity> transactions) {

        List<LocalDate> dates = transactions.stream()
                .map(TransactionSnapshotEntity::getTransactionDate)
                .toList();

        profile.setActiveDays(countUniqueDates(dates));
        profile.setTransactionsPerDay(calculateTransactionsPerDay(transactions.size(), getDateRange(dates)));
    }

    private void calculateClassificationFeatures(
            CustomerProfileEntity profile,
            List<TransactionSnapshotEntity> transactions) {

        int totalCount = transactions.size();

        // Cross-border
        int crossBorderCount = countByPredicate(transactions, TransactionSnapshotEntity::isCrossBorder);
        profile.setCrossBorderCount(crossBorderCount);
        profile.setCrossBorderRatio(calculateRatio(crossBorderCount, totalCount));

        // Cash transactions
        int cashCount = countByPredicate(transactions, TransactionSnapshotEntity::isCashTransaction);
        profile.setCashTransactionCount(cashCount);
        profile.setCashTransactionRatio(calculateRatio(cashCount, totalCount));

        // Large transactions
        int largeCount = countByPredicate(transactions, TransactionSnapshotEntity::isLargeTransaction);
        profile.setLargeTransactionCount(largeCount);
        profile.setLargeTransactionRatio(calculateRatio(largeCount, totalCount));

        // Night transactions
        int nightCount = countByPredicate(transactions, TransactionSnapshotEntity::isNightTransaction);
        profile.setNightTransactionCount(nightCount);
        profile.setNightTransactionRatio(calculateRatio(nightCount, totalCount));

        // Weekend transactions
        int weekendCount = countByPredicate(transactions, TransactionSnapshotEntity::isWeekendTransaction);
        profile.setWeekendTransactionCount(weekendCount);
        profile.setWeekendTransactionRatio(calculateRatio(weekendCount, totalCount));
    }

    private void calculateDiversityFeatures(
            CustomerProfileEntity profile,
            List<TransactionSnapshotEntity> transactions) {
        profile.setUniqueReceivers(countUniqueValues(transactions, TransactionSnapshotEntity::getReceiverAccountId));
        profile.setUniqueReceiverCountries(countUniqueValues(transactions, TransactionSnapshotEntity::getReceiverCountry));

        int uniqueCurrencies = (int) transactions.stream()
                .flatMap(t -> Stream.of(t.getFromCurrency(), t.getToCurrency()))
                .filter(Objects::nonNull)
                .distinct()
                .count();
        profile.setUniqueCurrencies(uniqueCurrencies);
        profile.setReceiverDiversity(calculateEntropyDiversity(transactions, TransactionSnapshotEntity::getReceiverAccountId));
    }

    private int countByPredicate(
            List<TransactionSnapshotEntity> transactions,
            Predicate<TransactionSnapshotEntity> predicate) {
        return (int) transactions.stream().filter(predicate).count();
    }
}