package org.banksolution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkFeaturesDto {

    private String accountId;
    private int uniqueInCounterparties;
    private int uniqueOutCounterparties;
    private double reciprocity;
    private int cycle3Count;
    private int twoHopOutReach;
    private double inOutAmountRatio;
    private double inConcentration;
    private double outConcentration;
    private double forwardingGapHours;
    private double peakDayShare;
}
