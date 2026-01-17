package org.banksolution.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.banksolution.dto.NetworkFeaturesDto;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NetworkGraphRepository {

    private final Driver neo4jDriver;

    public NetworkFeaturesDto getNetworkFeatures(String accountId) {
        try (Session session = neo4jDriver.session()) {
            String query = """
                    MATCH (a:Account {accountId: $accountId})
                    
                    // Count in-degree (incoming transactions)
                    OPTIONAL MATCH ()-[inRel:TRANSFERRED_TO]->(a)
                    WITH a, COUNT(DISTINCT inRel) AS inDegree
                    
                    // Count out-degree (outgoing transactions)
                    OPTIONAL MATCH (a)-[outRel:TRANSFERRED_TO]->()
                    WITH a, inDegree, COUNT(DISTINCT outRel) AS outDegree
                    
                    // Get total nodes for centrality calculation
                    OPTIONAL MATCH (total:Account)
                    WITH a, inDegree, outDegree, COUNT(DISTINCT total) AS totalNodes
                    
                    RETURN
                        a.accountId AS accountId,
                        inDegree,
                        outDegree,
                        CASE WHEN totalNodes > 1
                             THEN toFloat(inDegree + outDegree) / (totalNodes - 1)
                             ELSE 0.0 END AS degreeCentrality,
                        CASE WHEN totalNodes > 1
                             THEN toFloat(inDegree) / (totalNodes - 1)
                             ELSE 0.0 END AS inDegreeCentrality,
                        CASE WHEN totalNodes > 1
                             THEN toFloat(outDegree) / (totalNodes - 1)
                             ELSE 0.0 END AS outDegreeCentrality,
                        COALESCE(a.betweennessCentrality, 0.0) AS betweennessCentrality,
                        COALESCE(a.closenessCentrality, 0.0) AS closenessCentrality,
                        COALESCE(a.pagerank, 0.0) AS pagerank,
                        COALESCE(a.eigenvectorCentrality, 0.0) AS eigenvectorCentrality,
                        COALESCE(a.clusteringCoefficient, 0.0) AS clusteringCoefficient,
                        COALESCE(a.community, 0) AS community
                    """;

            Result result = session.run(query, Values.parameters("accountId", accountId));

            if (result.hasNext()) {
                Record resultNext = result.next();
                return NetworkFeaturesDto.builder()
                        .accountId(resultNext.get("accountId").asString())
                        .inDegree(resultNext.get("inDegree").asInt())
                        .outDegree(resultNext.get("outDegree").asInt())
                        .degreeCentrality(resultNext.get("degreeCentrality").asDouble())
                        .inDegreeCentrality(resultNext.get("inDegreeCentrality").asDouble())
                        .outDegreeCentrality(resultNext.get("outDegreeCentrality").asDouble())
                        .betweennessCentrality(resultNext.get("betweennessCentrality").asDouble())
                        .closenessCentrality(resultNext.get("closenessCentrality").asDouble())
                        .pagerank(resultNext.get("pagerank").asDouble())
                        .eigenvectorCentrality(resultNext.get("eigenvectorCentrality").asDouble())
                        .clusteringCoefficient(resultNext.get("clusteringCoefficient").asDouble())
                        .community(resultNext.get("community").asInt())
                        .build();
            }

            log.warn("Account not found in graph: {}", accountId);
            return NetworkFeaturesDto.defaultFeatures(accountId);
        }
    }

    public void createTransactionRelationship(
            String sourceAccountId,
            String destAccountId,
            String paymentId,
            double amount,
            String currency,
            String paymentType,
            long timestamp,
            boolean riskCheckPassed) {
        try (Session session = neo4jDriver.session()) {
            String query = """
                    MATCH (source:Account {accountId: $sourceAccountId})
                    MATCH (dest:Account {accountId: $destAccountId})
                    CREATE (source)-[r:TRANSFERRED_TO {
                        paymentId: $paymentId,
                        amount: $amount,
                        currency: $currency,
                        paymentType: $paymentType,
                        timestamp: $timestamp,
                        riskCheckPassed: $riskCheckPassed
                    }]->(dest)
                    """;

            session.run(query, Values.parameters(
                    "sourceAccountId", sourceAccountId,
                    "destAccountId", destAccountId,
                    "paymentId", paymentId,
                    "amount", amount,
                    "currency", currency,
                    "paymentType", paymentType,
                    "timestamp", timestamp,
                    "riskCheckPassed", riskCheckPassed
            ));

            log.debug("Transaction relationship created: {} -> {} for payment: {} riskCheckPassed: {}",
                    sourceAccountId,
                    destAccountId,
                    paymentId,
                    riskCheckPassed);
        }
    }
}
