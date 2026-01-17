package org.banksolution.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;

@Node("Account")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountNode {

    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;

    @Property("accountId")
    private String accountId;

    @Property("customerId")
    private String customerId;

    @Property("createdAt")
    private Instant createdAt;

    @Property("lastActivityAt")
    private Instant lastActivityAt;

    @Property("transactionCount")
    @Builder.Default
    private Integer transactionCount = 0;

    // Pre-computed centrality metrics (updated by scheduled jobs)
    @Property("pagerank")
    @Builder.Default
    private Double pagerank = 0.0;

    @Property("betweennessCentrality")
    @Builder.Default
    private Double betweennessCentrality = 0.0;

    @Property("closenessCentrality")
    @Builder.Default
    private Double closenessCentrality = 0.0;

    @Property("eigenvectorCentrality")
    @Builder.Default
    private Double eigenvectorCentrality = 0.0;

    @Property("clusteringCoefficient")
    @Builder.Default
    private Double clusteringCoefficient = 0.0;

    @Property("community")
    @Builder.Default
    private Integer community = 0;
}
