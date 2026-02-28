package org.banksolution.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphAlgorithmScheduledService {

    private final Driver neo4jDriver;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final int GDS_CONCURRENCY = 2;
    private static final String GRAPH_NAME_KEY = "graphName";
    private static final String GRAPH_NAME_DIRECTED = "transactionGraphDirected";
    private static final String GRAPH_NAME_UNDIRECTED = "transactionGraphUndirected";

    public void computeAllMetrics() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Graph algorithm computation already running — skipping trigger");
            return;
        }

        log.info("Starting scheduled graph algorithm computation");

        try {
            if (!hasEnoughNodes()) {
                log.info("Not enough nodes for graph algorithms, skipping");
                return;
            }

            // --- Directed algorithms: one projection in GDS memory at a time ---
            createProjection(GRAPH_NAME_DIRECTED, "NATURAL");
            computePageRank();
            computeBetweennessCentrality();
            computeEigenvectorCentrality();
            dropProjection(GRAPH_NAME_DIRECTED);

            // --- Undirected algorithms: directed projection already released ---
            createProjection(GRAPH_NAME_UNDIRECTED, "UNDIRECTED");
            computeClosenessCentrality();
            computeLocalClusteringCoefficient();
            computeCommunities();
            dropProjection(GRAPH_NAME_UNDIRECTED);

            log.info("Graph algorithm computation completed successfully");
        } catch (Exception e) {
            log.error("Failed to compute graph algorithms", e);
            safeDropProjection(GRAPH_NAME_DIRECTED);
            safeDropProjection(GRAPH_NAME_UNDIRECTED);
        } finally {
            running.set(false);
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

    private void createProjection(String graphName, String orientation) {
        try (Session session = neo4jDriver.session()) {
            session.run("CALL gds.graph.drop($graphName, false)",
                    Values.parameters(GRAPH_NAME_KEY, graphName)).consume();
            session.run("""
                    CALL gds.graph.project(
                        $graphName,
                        'Account',
                        {
                            TRANSFERRED_TO: {
                                orientation: $orientation,
                                properties: ['amount']
                            }
                        }
                    )
                    """, Values.parameters(GRAPH_NAME_KEY, graphName, "orientation", orientation)).consume();
            log.debug("{} projection created ({})", graphName, orientation);
        }
    }

    private void dropProjection(String graphName) {
        try (Session session = neo4jDriver.session()) {
            session.run("CALL gds.graph.drop($graphName, false)",
                    Values.parameters(GRAPH_NAME_KEY, graphName)).consume();
        }
    }

    private void safeDropProjection(String graphName) {
        try {
            dropProjection(graphName);
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    private void computePageRank() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.pageRank.write($graphName, {
                        writeProperty: 'pagerank',
                        dampingFactor: 0.85,
                        maxIterations: 20,
                        concurrency: $concurrency
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED, "concurrency", GDS_CONCURRENCY)).consume();
            log.debug("PageRank computed");
        }
    }

    private void computeBetweennessCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.betweenness.write($graphName, {
                        writeProperty: 'betweennessCentrality',
                        concurrency: $concurrency
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED, "concurrency", GDS_CONCURRENCY)).consume();
            log.debug("Betweenness centrality computed");
        }
    }

    private void computeEigenvectorCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.eigenvector.write($graphName, {
                        writeProperty: 'eigenvectorCentrality',
                        maxIterations: 20,
                        concurrency: $concurrency
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_DIRECTED, "concurrency", GDS_CONCURRENCY)).consume();
            log.debug("Eigenvector centrality computed");
        }
    }

    private void computeClosenessCentrality() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.closeness.write($graphName, {
                        writeProperty: 'closenessCentrality',
                        concurrency: $concurrency
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED, "concurrency", GDS_CONCURRENCY)).consume();
            log.debug("Closeness centrality computed");
        }
    }

    private void computeLocalClusteringCoefficient() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.localClusteringCoefficient.write($graphName, {
                        writeProperty: 'clusteringCoefficient',
                        concurrency: $concurrency
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED, "concurrency", GDS_CONCURRENCY)).consume();
            log.debug("Clustering coefficient computed");
        }
    }

    private void computeCommunities() {
        try (Session session = neo4jDriver.session()) {
            session.run("""
                    CALL gds.louvain.write($graphName, {
                        writeProperty: 'community',
                        concurrency: $concurrency
                    })
                    """, Values.parameters(GRAPH_NAME_KEY, GRAPH_NAME_UNDIRECTED, "concurrency", GDS_CONCURRENCY)).consume();
            log.debug("Community detection completed");
        }
    }
}