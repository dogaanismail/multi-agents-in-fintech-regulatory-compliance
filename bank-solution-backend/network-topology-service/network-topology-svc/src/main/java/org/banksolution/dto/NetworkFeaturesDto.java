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

    public static NetworkFeaturesDto defaultFeatures(String accountId) {
        return NetworkFeaturesDto.builder()
                .accountId(accountId)
                .inDegree(0)
                .outDegree(0)
                .degreeCentrality(0.0)
                .inDegreeCentrality(0.0)
                .outDegreeCentrality(0.0)
                .betweennessCentrality(0.0)
                .closenessCentrality(0.0)
                .pagerank(0.0)
                .eigenvectorCentrality(0.0)
                .clusteringCoefficient(0.0)
                .community(0)
                .build();
    }
}