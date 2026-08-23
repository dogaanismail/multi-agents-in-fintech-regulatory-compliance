package org.banksolution.service;

import lombok.experimental.UtilityClass;
import org.banksolution.domain.AccountMovement;
import org.banksolution.domain.AccountNeighbourhood;
import org.banksolution.dto.NetworkFeaturesDto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class NetworkFeatureCalculator {

    public static final double FORWARDING_GAP_CAP_HOURS = 168.0;
    private static final double IN_OUT_AMOUNT_RATIO_CAP = 1_000_000.0;
    private static final double MILLIS_PER_HOUR = 3_600_000.0;

    public NetworkFeaturesDto toNetworkFeatures(AccountNeighbourhood neighbourhood) {
        return NetworkFeaturesDto.builder()
                .accountId(neighbourhood.accountId())
                .uniqueInCounterparties(neighbourhood.senderAccountIds().size())
                .uniqueOutCounterparties(neighbourhood.receiverAccountIds().size())
                .reciprocity(calculateReciprocity(
                        neighbourhood.senderAccountIds(), neighbourhood.receiverAccountIds()))
                .cycle3Count(neighbourhood.cycle3Count())
                .twoHopOutReach(neighbourhood.twoHopOutReach())
                .inOutAmountRatio(calculateInOutAmountRatio(
                        neighbourhood.incomingMovements(), neighbourhood.outgoingMovements()))
                .inConcentration(calculateAmountConcentrationByCounterparty(
                        neighbourhood.incomingMovements()))
                .outConcentration(calculateAmountConcentrationByCounterparty(
                        neighbourhood.outgoingMovements()))
                .forwardingGapHours(calculateForwardingGapHours(
                        neighbourhood.incomingMovements(), neighbourhood.outgoingMovements()))
                .peakDayShare(calculatePeakDayShare(
                        neighbourhood.incomingMovements(), neighbourhood.outgoingMovements()))
                .build();
    }

    public double calculateReciprocity(
            Set<String> senderAccountIds,
            Set<String> receiverAccountIds) {

        Set<String> reciprocalCounterparties = new HashSet<>(senderAccountIds);
        reciprocalCounterparties.retainAll(receiverAccountIds);
        int union = senderAccountIds.size() + receiverAccountIds.size() - reciprocalCounterparties.size();
        return union == 0 ? 0.0 : (double) reciprocalCounterparties.size() / union;
    }

    public double calculateInOutAmountRatio(
            List<AccountMovement> incomingMovements,
            List<AccountMovement> outgoingMovements) {

        double totalIncomingAmount = sumAmounts(incomingMovements);
        double totalOutgoingAmount = sumAmounts(outgoingMovements);
        return Math.min(totalOutgoingAmount / (totalIncomingAmount + 1.0), IN_OUT_AMOUNT_RATIO_CAP);
    }

    public double calculateAmountConcentrationByCounterparty(List<AccountMovement> movements) {
        Map<String, Double> amountByCounterparty = new HashMap<>();
        for (AccountMovement movement : movements) {
            amountByCounterparty.merge(movement.counterpartyAccountId(), movement.amount(), Double::sum);
        }

        double totalAmount = amountByCounterparty.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalAmount == 0.0) {
            return 0.0;
        }

        return amountByCounterparty.values().stream()
                .mapToDouble(amount -> {
                    double share = amount / totalAmount;
                    return share * share;
                })
                .sum();
    }

    public double calculateForwardingGapHours(
            List<AccountMovement> incomingMovements,
            List<AccountMovement> outgoingMovements) {

        if (incomingMovements.isEmpty() || outgoingMovements.isEmpty()) {
            return FORWARDING_GAP_CAP_HOURS;
        }

        long[] incomingTimestamps = sortedTimestamps(incomingMovements);
        List<Double> gapHours = new ArrayList<>();
        for (AccountMovement outgoing : outgoingMovements) {
            long latestIncomingBefore = latestTimestampAtOrBefore(
                    incomingTimestamps, outgoing.timestampEpochMillis());
            if (latestIncomingBefore != Long.MIN_VALUE) {
                gapHours.add((outgoing.timestampEpochMillis() - latestIncomingBefore) / MILLIS_PER_HOUR);
            }
        }

        if (gapHours.isEmpty()) {
            return FORWARDING_GAP_CAP_HOURS;
        }

        return Math.min(median(gapHours), FORWARDING_GAP_CAP_HOURS);
    }

    public double calculatePeakDayShare(
            List<AccountMovement> incomingMovements,
            List<AccountMovement> outgoingMovements) {

        Map<LocalDate, Long> paymentsPerDay = new HashMap<>();
        countPaymentsPerDay(incomingMovements, paymentsPerDay);
        countPaymentsPerDay(outgoingMovements, paymentsPerDay);
        if (paymentsPerDay.isEmpty()) {
            return 0.0;
        }

        long busiestDayCount = paymentsPerDay.values().stream().mapToLong(Long::longValue).max().orElse(0);
        long totalCount = paymentsPerDay.values().stream().mapToLong(Long::longValue).sum();

        return (double) busiestDayCount / totalCount;
    }

    private double sumAmounts(List<AccountMovement> movements) {
        return movements.stream().mapToDouble(AccountMovement::amount).sum();
    }

    private long[] sortedTimestamps(List<AccountMovement> movements) {
        long[] timestamps = movements.stream()
                .mapToLong(AccountMovement::timestampEpochMillis)
                .toArray();
        Arrays.sort(timestamps);
        return timestamps;
    }

    private long latestTimestampAtOrBefore(long[] sortedTimestamps, long limit) {
        int insertionPoint = Arrays.binarySearch(sortedTimestamps, limit);
        if (insertionPoint >= 0) {
            return sortedTimestamps[insertionPoint];
        }
        int firstGreater = -insertionPoint - 1;
        return firstGreater == 0 ? Long.MIN_VALUE : sortedTimestamps[firstGreater - 1];
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }

        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private void countPaymentsPerDay(List<AccountMovement> movements, Map<LocalDate, Long> paymentsPerDay) {
        for (AccountMovement movement : movements) {
            LocalDate day = Instant.ofEpochMilli(movement.timestampEpochMillis())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            paymentsPerDay.merge(day, 1L, Long::sum);
        }
    }
}
