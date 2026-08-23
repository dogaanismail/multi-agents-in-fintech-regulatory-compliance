package org.banksolution.service;

import org.banksolution.domain.AccountMovement;
import org.banksolution.domain.AccountNeighbourhood;
import org.banksolution.dto.NetworkFeaturesDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.banksolution.service.fixtures.AccountNeighbourhoodFixtures.ACCOUNT_ID;
import static org.banksolution.service.fixtures.AccountNeighbourhoodFixtures.createMovement;

class NetworkFeatureCalculatorTest {

    @Test
    void shouldCalculateReciprocityAsIntersectionOverUnionOfCounterparties() {
        double reciprocity = NetworkFeatureCalculator.calculateReciprocity(
                Set.of("a", "b", "c"),
                Set.of("b", "c", "d"));

        assertThat(reciprocity).isEqualTo(2.0 / 4.0);
    }

    @Test
    void shouldCalculateZeroReciprocityWhenAccountHasNoCounterparties() {
        double reciprocity = NetworkFeatureCalculator.calculateReciprocity(Set.of(), Set.of());

        assertThat(reciprocity).isZero();
    }

    @Test
    void shouldCalculateInOutAmountRatioAsOutgoingOverIncomingPlusOne() {
        List<AccountMovement> incoming = List.of(
                createMovement("sender-1", 600.0, "2026-08-01T10:00:00Z"),
                createMovement("sender-2", 399.0, "2026-08-01T11:00:00Z"));
        List<AccountMovement> outgoing = List.of(
                createMovement("receiver-1", 500.0, "2026-08-01T12:00:00Z"));

        double ratio = NetworkFeatureCalculator.calculateInOutAmountRatio(incoming, outgoing);

        assertThat(ratio).isEqualTo(500.0 / 1000.0);
    }

    @Test
    void shouldCapInOutAmountRatioForAccountsThatOnlySend() {
        List<AccountMovement> outgoing = List.of(
                createMovement("receiver-1", 2_000_000_000.0, "2026-08-01T12:00:00Z"));

        double ratio = NetworkFeatureCalculator.calculateInOutAmountRatio(List.of(), outgoing);

        assertThat(ratio).isEqualTo(1_000_000.0);
    }

    @Test
    void shouldCalculateConcentrationAsHerfindahlOverCounterpartyAmounts() {
        List<AccountMovement> movements = List.of(
                createMovement("counterparty-1", 50.0, "2026-08-01T10:00:00Z"),
                createMovement("counterparty-1", 25.0, "2026-08-01T11:00:00Z"),
                createMovement("counterparty-2", 25.0, "2026-08-01T12:00:00Z"));

        double concentration = NetworkFeatureCalculator.calculateAmountConcentrationByCounterparty(movements);

        assertThat(concentration).isCloseTo(0.75 * 0.75 + 0.25 * 0.25, within(1e-12));
    }

    @Test
    void shouldCalculateZeroConcentrationWithoutMovements() {
        double concentration = NetworkFeatureCalculator.calculateAmountConcentrationByCounterparty(List.of());

        assertThat(concentration).isZero();
    }

    @Test
    void shouldCalculateForwardingGapAsMedianHoursFromLatestIncomingBeforeEachOutgoing() {
        List<AccountMovement> incoming = List.of(
                createMovement("sender-1", 100.0, "2026-08-01T10:00:00Z"),
                createMovement("sender-1", 100.0, "2026-08-01T14:00:00Z"));
        List<AccountMovement> outgoing = List.of(
                createMovement("receiver-1", 100.0, "2026-08-01T11:00:00Z"),
                createMovement("receiver-1", 100.0, "2026-08-01T17:00:00Z"));

        double gapHours = NetworkFeatureCalculator.calculateForwardingGapHours(incoming, outgoing);

        assertThat(gapHours).isEqualTo((1.0 + 3.0) / 2.0);
    }

    @Test
    void shouldCapForwardingGapWhenEitherDirectionIsMissing() {
        List<AccountMovement> outgoing = List.of(
                createMovement("receiver-1", 100.0, "2026-08-01T11:00:00Z"));

        double gapHours = NetworkFeatureCalculator.calculateForwardingGapHours(List.of(), outgoing);

        assertThat(gapHours).isEqualTo(NetworkFeatureCalculator.FORWARDING_GAP_CAP_HOURS);
    }

    @Test
    void shouldCapForwardingGapWhenEveryOutgoingPrecedesTheFirstIncoming() {
        List<AccountMovement> incoming = List.of(
                createMovement("sender-1", 100.0, "2026-08-02T10:00:00Z"));
        List<AccountMovement> outgoing = List.of(
                createMovement("receiver-1", 100.0, "2026-08-01T09:00:00Z"));

        double gapHours = NetworkFeatureCalculator.calculateForwardingGapHours(incoming, outgoing);

        assertThat(gapHours).isEqualTo(NetworkFeatureCalculator.FORWARDING_GAP_CAP_HOURS);
    }

    @Test
    void shouldCalculatePeakDayShareOverIncomingAndOutgoingTogether() {
        List<AccountMovement> incoming = List.of(
                createMovement("sender-1", 100.0, "2026-08-01T10:00:00Z"),
                createMovement("sender-1", 100.0, "2026-08-01T11:00:00Z"),
                createMovement("sender-1", 100.0, "2026-08-02T10:00:00Z"));
        List<AccountMovement> outgoing = List.of(
                createMovement("receiver-1", 100.0, "2026-08-01T12:00:00Z"));

        double peakDayShare = NetworkFeatureCalculator.calculatePeakDayShare(incoming, outgoing);

        assertThat(peakDayShare).isEqualTo(3.0 / 4.0);
    }

    @Test
    void shouldMapEmptyNeighbourhoodToNeutralFeatures() {
        NetworkFeaturesDto features = NetworkFeatureCalculator.toNetworkFeatures(
                AccountNeighbourhood.empty(ACCOUNT_ID));

        assertThat(features.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(features.getUniqueInCounterparties()).isZero();
        assertThat(features.getUniqueOutCounterparties()).isZero();
        assertThat(features.getReciprocity()).isZero();
        assertThat(features.getCycle3Count()).isZero();
        assertThat(features.getTwoHopOutReach()).isZero();
        assertThat(features.getInOutAmountRatio()).isZero();
        assertThat(features.getInConcentration()).isZero();
        assertThat(features.getOutConcentration()).isZero();
        assertThat(features.getForwardingGapHours())
                .isEqualTo(NetworkFeatureCalculator.FORWARDING_GAP_CAP_HOURS);
        assertThat(features.getPeakDayShare()).isZero();
    }

    @Test
    void shouldMapNeighbourhoodCountsOntoFeatureFields() {
        AccountNeighbourhood neighbourhood = new AccountNeighbourhood(
                ACCOUNT_ID,
                Set.of("sender-1", "shared"),
                Set.of("receiver-1", "shared"),
                3,
                7,
                List.of(createMovement("sender-1", 900.0, "2026-08-01T10:00:00Z")),
                List.of(createMovement("receiver-1", 450.0, "2026-08-01T12:00:00Z")));

        NetworkFeaturesDto features = NetworkFeatureCalculator.toNetworkFeatures(neighbourhood);

        assertThat(features.getUniqueInCounterparties()).isEqualTo(2);
        assertThat(features.getUniqueOutCounterparties()).isEqualTo(2);
        assertThat(features.getReciprocity()).isEqualTo(1.0 / 3.0);
        assertThat(features.getCycle3Count()).isEqualTo(3);
        assertThat(features.getTwoHopOutReach()).isEqualTo(7);
        assertThat(features.getInOutAmountRatio()).isEqualTo(450.0 / 901.0);
        assertThat(features.getInConcentration()).isEqualTo(1.0);
        assertThat(features.getOutConcentration()).isEqualTo(1.0);
        assertThat(features.getForwardingGapHours()).isEqualTo(2.0);
        assertThat(features.getPeakDayShare()).isEqualTo(1.0);
    }
}
