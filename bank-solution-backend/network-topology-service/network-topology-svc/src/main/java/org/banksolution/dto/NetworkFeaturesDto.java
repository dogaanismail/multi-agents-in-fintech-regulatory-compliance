package org.banksolution.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Graph features of an account in the payment network")
public class NetworkFeaturesDto {

    @Schema(description = "Account the features describe", example = "b1e4f7a2-5c8d-4a3b-9e6f-0d2c7a9b4e15")
    private String accountId;

    @Schema(description = "Number of incoming payment edges", example = "4")
    private int inDegree;

    @Schema(description = "Number of outgoing payment edges", example = "7")
    private int outDegree;

    @Schema(description = "Normalised total degree centrality", example = "0.03")
    private double degreeCentrality;

    @Schema(description = "Normalised incoming degree centrality", example = "0.01")
    private double inDegreeCentrality;

    @Schema(description = "Normalised outgoing degree centrality", example = "0.02")
    private double outDegreeCentrality;

    @Schema(description = "Betweenness centrality in the payment graph", example = "0.004")
    private double betweennessCentrality;

    @Schema(description = "Closeness centrality in the payment graph", example = "0.21")
    private double closenessCentrality;

    @Schema(description = "PageRank score in the payment graph", example = "0.15")
    private double pagerank;

    @Schema(description = "Eigenvector centrality in the payment graph", example = "0.08")
    private double eigenvectorCentrality;

    @Schema(description = "Local clustering coefficient", example = "0.33")
    private double clusteringCoefficient;

    @Schema(description = "Detected community id", example = "3")
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