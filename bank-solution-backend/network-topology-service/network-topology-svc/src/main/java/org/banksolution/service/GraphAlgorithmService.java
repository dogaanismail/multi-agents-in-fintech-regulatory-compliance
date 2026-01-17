package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphAlgorithmService {

    private final Driver neo4jDriver;
    private static final String GRAPH_NAME_KEY = "graphName";
    private static final String GRAPH_NAME = "transactionGraph";

    @Value("${app.graph.algorithm.enabled:true}")
    private boolean algorithmEnabled;

    @Scheduled(fixedRateString = "${app.graph.algorithm.interval-ms:300000}", initialDelay = 60000)
    public void computeAllMetrics() {
        if (!algorithmEnabled) {
            log.debug("Graph algorithms disabled, skipping computation");
            return;
        }

        log.info("Starting scheduled graph algorithm computation");

        try {
            if (!hasEnoughNodes()) {
                log.info("Not enough nodes for graph algorithms, skipping");
                return;
            }

            refreshGraphProjection();
            computePageRank();
            computeBetweennessCentrality();
            computeCommunities();
            dropGraphProjection();

            log.info("Graph algorithm computation completed successfully");
        } catch (Exception e) {
            log.error("Failed to compute graph algorithms", e);
        }
    }

    private boolean hasEnoughNodes() {
        try (Session session = neo4jDriver.session()) {
            Result result = session.run("MATCH (a:Account) RETURN COUNT(a) AS count");
            if (result.hasNext()) {
                long count = result.next().get("count").asLong();
                return count >= 2;
            }
            return false;
        }
    }

    private void refreshGraphProjection() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.graph.drop($graphName, false)
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME));

            session.run("""
                    CALL gds.graph.project(
                        $graphName,
                        'Account',
                        {
                            TRANSFERRED_TO: {
                                orientation: 'NATURAL',
                                properties: ['amount']
                            }
                        }
                    )
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME));
            log.debug("Graph projection created");
        }
    }

    private void computePageRank() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.pageRank.write($graphName, {
                        writeProperty: 'pagerank',
                        dampingFactor: 0.85,
                        maxIterations: 20
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME));
            log.debug("PageRank computed");
        }
    }

    private void computeBetweennessCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.betweenness.write($graphName, {
                        writeProperty: 'betweennessCentrality'
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME));
            log.debug("Betweenness centrality computed");
        }
    }

    private void computeCommunities() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.louvain.write($graphName, {
                        writeProperty: 'community'
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME));
            log.debug("Community detection completed");
        }
    }

    private void dropGraphProjection() {
        try (Session session = neo4jDriver.session()) {
            session.run("CALL gds.graph.drop($graphName, false)", Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME));
        }
    }
}
