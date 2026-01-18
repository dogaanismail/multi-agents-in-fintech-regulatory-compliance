package org.banksolution.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class StatisticsUtil {

    private static final int DECIMAL_SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    // ==================== Amount Statistics ====================

    public BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal mean(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return sum(values).divide(
                new BigDecimal(values.size()),
                DECIMAL_SCALE,
                ROUNDING_MODE
        );
    }

    public BigDecimal max(List<BigDecimal> values) {
        return values.stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal min(List<BigDecimal> values) {
        return values.stream()
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int size = sorted.size();

        if (size % 2 == 0) {
            return sorted.get(size / 2 - 1)
                    .add(sorted.get(size / 2))
                    .divide(new BigDecimal(2), DECIMAL_SCALE, ROUNDING_MODE);
        } else {
            return sorted.get(size / 2);
        }
    }

    public BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal mean) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value.subtract(mean).doubleValue(), 2))
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(Math.sqrt(variance))
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    public BigDecimal calculateConsistency(BigDecimal stdDev, BigDecimal mean) {
        if (mean.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal coefficientOfVariation = stdDev.divide(mean, DECIMAL_SCALE, ROUNDING_MODE);
        return BigDecimal.ONE.subtract(coefficientOfVariation)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }

    // ==================== Ratio Calculations ====================

    public BigDecimal calculateRatio(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(count)
                .divide(new BigDecimal(total), DECIMAL_SCALE, ROUNDING_MODE);
    }

    // ==================== Temporal Statistics ====================

    public int countUniqueDates(List<LocalDate> dates) {
        return (int) dates.stream()
                .distinct()
                .count();
    }

    public long getDateRange(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 1;
        }

        LocalDate firstDate = dates.stream()
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate lastDate = dates.stream()
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        return ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
    }

    public BigDecimal calculateTransactionsPerDay(int transactionCount, long dayRange) {
        if (dayRange == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(transactionCount)
                .divide(new BigDecimal(dayRange), DECIMAL_SCALE, ROUNDING_MODE);
    }

    // ==================== Diversity Statistics ====================

    public <T> int countUniqueValues(List<T> items, Function<T, String> extractor) {
        return (int) items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    public <T> BigDecimal calculateEntropyDiversity(List<T> items, Function<T, String> extractor) {
        Map<String, Long> valueCounts = items.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        if (valueCounts.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int total = items.size();
        double entropy = valueCounts.values().stream()
                .mapToDouble(count -> {
                    double probability = count.doubleValue() / total;
                    return -probability * (Math.log(probability) / Math.log(2));
                })
                .sum();

        double maxEntropy = Math.log(valueCounts.size()) / Math.log(2);

        if (maxEntropy == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(entropy / maxEntropy)
                .setScale(DECIMAL_SCALE, ROUNDING_MODE);
    }
}