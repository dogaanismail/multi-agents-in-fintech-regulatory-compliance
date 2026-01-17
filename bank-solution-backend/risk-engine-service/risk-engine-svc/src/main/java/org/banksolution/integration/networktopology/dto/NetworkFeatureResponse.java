package org.banksolution.integration.networktopology.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkFeatureResponse {

    private String accountId;
    private int inDegree;
    private int outDegree;
    private double degreeCentrality;
    private double inDegreeCentrality;
    private double outDegreeCentrality;
    private double betweennessCentrality;
    private double closenessCentrality;
    private double pagerank;
    private double eigenvectorCentrality;
    private double clusteringCoefficient;
    private int community;
}
