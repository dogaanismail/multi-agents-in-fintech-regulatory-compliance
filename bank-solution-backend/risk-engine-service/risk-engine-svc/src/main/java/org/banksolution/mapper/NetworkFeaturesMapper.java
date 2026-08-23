package org.banksolution.mapper;

import com.aml.fraud.NetworkFeatures;
import lombok.experimental.UtilityClass;
import org.banksolution.integration.networktopology.dto.NetworkFeatureResponse;

@UtilityClass
public class NetworkFeaturesMapper {

    // Matches the topology service's forwarding-gap cap: an unknown account must
    // look like one with no incoming/outgoing history, not like an instant forwarder.
    private static final double FORWARDING_GAP_CAP_HOURS = 168.0;

    public NetworkFeatures toAvroNetworkFeatures(NetworkFeatureResponse response) {
        return NetworkFeatures.newBuilder()
                .setAccountId(response.getAccountId())
                .setUniqueInCounterparties(response.getUniqueInCounterparties())
                .setUniqueOutCounterparties(response.getUniqueOutCounterparties())
                .setReciprocity(response.getReciprocity())
                .setCycle3Count(response.getCycle3Count())
                .setTwoHopOutReach(response.getTwoHopOutReach())
                .setInOutAmountRatio(response.getInOutAmountRatio())
                .setInConcentration(response.getInConcentration())
                .setOutConcentration(response.getOutConcentration())
                .setForwardingGapHours(response.getForwardingGapHours())
                .setPeakDayShare(response.getPeakDayShare())
                .build();
    }

    public NetworkFeatures getDefaultNetworkFeatures(String accountId) {
        return NetworkFeatures.newBuilder()
                .setAccountId(accountId)
                .setUniqueInCounterparties(0)
                .setUniqueOutCounterparties(0)
                .setReciprocity(0.0)
                .setCycle3Count(0)
                .setTwoHopOutReach(0)
                .setInOutAmountRatio(0.0)
                .setInConcentration(0.0)
                .setOutConcentration(0.0)
                .setForwardingGapHours(FORWARDING_GAP_CAP_HOURS)
                .setPeakDayShare(0.0)
                .build();
    }
}
